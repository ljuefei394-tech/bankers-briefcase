package com.gec.deal.game;

public enum GameStatus {
    SELECTING_PERSONAL_CASE,
    OPENING_CASES,
    BANKER_OFFER,
    FINAL_CHOICE,
    DEAL_ACCEPTED,
    FINISHED;

    public boolean isTerminal() {
        return this == DEAL_ACCEPTED || this == FINISHED;
    }
}
