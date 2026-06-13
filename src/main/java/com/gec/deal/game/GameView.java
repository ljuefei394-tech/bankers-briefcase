package com.gec.deal.game;

import java.math.BigDecimal;
import java.util.List;

public record GameView(
        String id,
        GameStatus status,
        int round,
        int openedInRound,
        int casesToOpenThisRound,
        int casesLeftToOpen,
        int personalCaseNumber,
        List<BriefcaseView> cases,
        List<BigDecimal> openedAmounts,
        List<BigDecimal> remainingAmounts,
        BankerOffer currentOffer,
        BigDecimal acceptedOffer,
        BigDecimal finalPrize,
        boolean switchedAtFinal
) {
}
