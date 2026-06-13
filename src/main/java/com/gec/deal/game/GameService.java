package com.gec.deal.game;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {

    private static final List<BigDecimal> PRIZE_AMOUNTS = List.of(
            new BigDecimal("0.01"),
            new BigDecimal("1"),
            new BigDecimal("5"),
            new BigDecimal("10"),
            new BigDecimal("25"),
            new BigDecimal("50"),
            new BigDecimal("75"),
            new BigDecimal("100"),
            new BigDecimal("200"),
            new BigDecimal("300"),
            new BigDecimal("400"),
            new BigDecimal("500"),
            new BigDecimal("750"),
            new BigDecimal("1000"),
            new BigDecimal("5000"),
            new BigDecimal("10000"),
            new BigDecimal("25000"),
            new BigDecimal("50000"),
            new BigDecimal("75000"),
            new BigDecimal("100000"),
            new BigDecimal("200000"),
            new BigDecimal("300000"),
            new BigDecimal("400000"),
            new BigDecimal("500000"),
            new BigDecimal("750000"),
            new BigDecimal("1000000")
    );
    private static final int[] CASES_TO_OPEN_BY_ROUND = {6, 5, 4, 3, 2, 1, 1, 1, 1};
    private static final BigDecimal[] OFFER_FACTORS = {
            new BigDecimal("0.48"),
            new BigDecimal("0.56"),
            new BigDecimal("0.64"),
            new BigDecimal("0.72"),
            new BigDecimal("0.80"),
            new BigDecimal("0.88"),
            new BigDecimal("0.94"),
            new BigDecimal("0.98"),
            new BigDecimal("1.00")
    };

    private final Map<String, GameSession> games = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public GameView createGame() {
        List<BigDecimal> shuffledAmounts = new ArrayList<>(PRIZE_AMOUNTS);
        Collections.shuffle(shuffledAmounts, random);

        List<Briefcase> cases = new ArrayList<>();
        for (int i = 0; i < shuffledAmounts.size(); i++) {
            cases.add(new Briefcase(i + 1, shuffledAmounts.get(i)));
        }

        String gameId = UUID.randomUUID().toString();
        GameSession session = new GameSession(gameId, cases);
        games.put(gameId, session);
        return toView(session);
    }

    public GameView getGame(String gameId) {
        return toView(findGame(gameId));
    }

    public GameView selectPersonalCase(String gameId, int caseNumber) {
        GameSession session = findGame(gameId);
        if (session.getStatus() != GameStatus.SELECTING_PERSONAL_CASE) {
            throw new GameException("当前不能选择专属箱子。");
        }

        Briefcase briefcase = findCase(session, caseNumber);
        session.setPersonalCaseNumber(briefcase.getNumber());
        session.setStatus(GameStatus.OPENING_CASES);
        return toView(session);
    }

    public GameView openCase(String gameId, int caseNumber) {
        GameSession session = findGame(gameId);
        if (session.getStatus() != GameStatus.OPENING_CASES) {
            throw new GameException("当前不能开箱。");
        }
        if (session.getPersonalCaseNumber() == caseNumber) {
            throw new GameException("专属箱子不能提前打开。");
        }

        Briefcase briefcase = findCase(session, caseNumber);
        if (briefcase.isOpened()) {
            throw new GameException("这个箱子已经打开过。");
        }

        briefcase.open();
        session.setOpenedInRound(session.getOpenedInRound() + 1);
        if (remainingUnopenedCaseCount(session) == 1) {
            session.setStatus(GameStatus.FINAL_CHOICE);
        } else if (session.getOpenedInRound() >= casesToOpenThisRound(session)) {
            session.setCurrentOffer(calculateOffer(session));
            session.setStatus(GameStatus.BANKER_OFFER);
        }
        return toView(session);
    }

    public GameView acceptDeal(String gameId) {
        GameSession session = findGame(gameId);
        if (session.getStatus() != GameStatus.BANKER_OFFER || session.getCurrentOffer() == null) {
            throw new GameException("当前没有可接受的银行报价。");
        }

        session.setAcceptedOffer(session.getCurrentOffer().amount());
        session.setStatus(GameStatus.DEAL_ACCEPTED);
        revealAllCases(session);
        return toView(session);
    }

    public GameView rejectDeal(String gameId) {
        GameSession session = findGame(gameId);
        if (session.getStatus() != GameStatus.BANKER_OFFER) {
            throw new GameException("当前没有银行报价可拒绝。");
        }

        session.setRound(session.getRound() + 1);
        session.setOpenedInRound(0);
        session.setCurrentOffer(null);
        session.setStatus(GameStatus.OPENING_CASES);
        return toView(session);
    }

    public GameView keepPersonalCase(String gameId) {
        GameSession session = findGame(gameId);
        if (session.getStatus() != GameStatus.FINAL_CHOICE) {
            throw new GameException("现在还不能做最终选择。");
        }

        BigDecimal prize = findCase(session, session.getPersonalCaseNumber()).getAmount();
        session.setFinalPrize(prize);
        session.setStatus(GameStatus.FINISHED);
        revealAllCases(session);
        return toView(session);
    }

    public GameView switchToLastCase(String gameId) {
        GameSession session = findGame(gameId);
        if (session.getStatus() != GameStatus.FINAL_CHOICE) {
            throw new GameException("现在还不能做最终选择。");
        }

        Briefcase lastCase = session.getCases().stream()
                .filter(briefcase -> !briefcase.isOpened())
                .filter(briefcase -> briefcase.getNumber() != session.getPersonalCaseNumber())
                .findFirst()
                .orElseThrow(() -> new GameException("没有可交换的箱子。"));

        session.setFinalPrize(lastCase.getAmount());
        session.setSwitchedAtFinal(true);
        session.setStatus(GameStatus.FINISHED);
        revealAllCases(session);
        return toView(session);
    }

    private GameSession findGame(String gameId) {
        GameSession session = games.get(gameId);
        if (session == null) {
            throw new GameException("游戏不存在，请重新开始。");
        }
        return session;
    }

    private Briefcase findCase(GameSession session, int caseNumber) {
        return session.getCases().stream()
                .filter(briefcase -> briefcase.getNumber() == caseNumber)
                .findFirst()
                .orElseThrow(() -> new GameException("箱子编号无效。"));
    }

    private int casesToOpenThisRound(GameSession session) {
        int roundIndex = Math.min(session.getRound() - 1, CASES_TO_OPEN_BY_ROUND.length - 1);
        return CASES_TO_OPEN_BY_ROUND[roundIndex];
    }

    private int remainingUnopenedCaseCount(GameSession session) {
        return (int) session.getCases().stream()
                .filter(briefcase -> !briefcase.isOpened())
                .filter(briefcase -> briefcase.getNumber() != session.getPersonalCaseNumber())
                .count();
    }

    private BankerOffer calculateOffer(GameSession session) {
        List<BigDecimal> remainingAmounts = session.getCases().stream()
                .filter(briefcase -> !briefcase.isOpened())
                .map(Briefcase::getAmount)
                .toList();
        BigDecimal total = remainingAmounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expectedValue = total.divide(BigDecimal.valueOf(remainingAmounts.size()), 2, RoundingMode.HALF_UP);

        int roundIndex = Math.min(session.getRound() - 1, OFFER_FACTORS.length - 1);
        BigDecimal offer = expectedValue.multiply(OFFER_FACTORS[roundIndex]).setScale(0, RoundingMode.HALF_UP);
        return new BankerOffer(offer, session.getRound(), openedCaseCount(session), remainingAmounts.size());
    }

    private int openedCaseCount(GameSession session) {
        return (int) session.getCases().stream().filter(Briefcase::isOpened).count();
    }

    private void revealAllCases(GameSession session) {
        session.getCases().forEach(Briefcase::open);
    }

    private GameView toView(GameSession session) {
        boolean revealAmounts = session.getStatus().isTerminal();
        List<BriefcaseView> caseViews = session.getCases().stream()
                .sorted(Comparator.comparingInt(Briefcase::getNumber))
                .map(briefcase -> new BriefcaseView(
                        briefcase.getNumber(),
                        briefcase.getNumber() == session.getPersonalCaseNumber(),
                        briefcase.isOpened(),
                        revealAmounts || briefcase.isOpened() ? briefcase.getAmount() : null
                ))
                .toList();

        List<BigDecimal> openedAmounts = session.getCases().stream()
                .filter(Briefcase::isOpened)
                .map(Briefcase::getAmount)
                .sorted()
                .toList();

        List<BigDecimal> remainingAmounts = PRIZE_AMOUNTS.stream()
                .filter(amount -> !openedAmounts.contains(amount))
                .sorted()
                .toList();

        return new GameView(
                session.getId(),
                session.getStatus(),
                session.getRound(),
                session.getOpenedInRound(),
                casesToOpenThisRound(session),
                Math.max(0, casesToOpenThisRound(session) - session.getOpenedInRound()),
                session.getPersonalCaseNumber(),
                caseViews,
                openedAmounts,
                remainingAmounts,
                session.getCurrentOffer(),
                session.getAcceptedOffer(),
                session.getFinalPrize(),
                session.isSwitchedAtFinal()
        );
    }
}
