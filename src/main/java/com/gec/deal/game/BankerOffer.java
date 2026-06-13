package com.gec.deal.game;

import java.math.BigDecimal;

public record BankerOffer(
        BigDecimal amount,
        int round,
        int openedCount,
        int remainingCount
) {
}
