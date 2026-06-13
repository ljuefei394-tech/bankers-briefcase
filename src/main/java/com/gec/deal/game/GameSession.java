package com.gec.deal.game;

import java.math.BigDecimal;
import java.util.List;

public class GameSession {

    private final String id;
    private final List<Briefcase> cases;
    private GameStatus status = GameStatus.SELECTING_PERSONAL_CASE;
    private int round = 1;
    private int openedInRound;
    private int personalCaseNumber;
    private BankerOffer currentOffer;
    private BigDecimal acceptedOffer;
    private BigDecimal finalPrize;
    private boolean switchedAtFinal;

    public GameSession(String id, List<Briefcase> cases) {
        this.id = id;
        this.cases = cases;
    }

    public String getId() {
        return id;
    }

    public List<Briefcase> getCases() {
        return cases;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public int getOpenedInRound() {
        return openedInRound;
    }

    public void setOpenedInRound(int openedInRound) {
        this.openedInRound = openedInRound;
    }

    public int getPersonalCaseNumber() {
        return personalCaseNumber;
    }

    public void setPersonalCaseNumber(int personalCaseNumber) {
        this.personalCaseNumber = personalCaseNumber;
    }

    public BankerOffer getCurrentOffer() {
        return currentOffer;
    }

    public void setCurrentOffer(BankerOffer currentOffer) {
        this.currentOffer = currentOffer;
    }

    public BigDecimal getAcceptedOffer() {
        return acceptedOffer;
    }

    public void setAcceptedOffer(BigDecimal acceptedOffer) {
        this.acceptedOffer = acceptedOffer;
    }

    public BigDecimal getFinalPrize() {
        return finalPrize;
    }

    public void setFinalPrize(BigDecimal finalPrize) {
        this.finalPrize = finalPrize;
    }

    public boolean isSwitchedAtFinal() {
        return switchedAtFinal;
    }

    public void setSwitchedAtFinal(boolean switchedAtFinal) {
        this.switchedAtFinal = switchedAtFinal;
    }
}
