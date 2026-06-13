package com.gec.deal.game;

import java.math.BigDecimal;

public record BriefcaseView(
        int number,
        boolean selected,
        boolean opened,
        BigDecimal amount
) {
}
