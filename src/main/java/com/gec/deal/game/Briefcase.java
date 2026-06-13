package com.gec.deal.game;

import java.math.BigDecimal;

public class Briefcase {

    private final int number;
    private final BigDecimal amount;
    private boolean opened;

    public Briefcase(int number, BigDecimal amount) {
        this.number = number;
        this.amount = amount;
    }

    public int getNumber() {
        return number;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public boolean isOpened() {
        return opened;
    }

    public void open() {
        this.opened = true;
    }
}
