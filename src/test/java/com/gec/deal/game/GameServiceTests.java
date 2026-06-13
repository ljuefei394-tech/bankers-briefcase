package com.gec.deal.game;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameServiceTests {

    private final GameService gameService = new GameService();

    @Test
    void shouldCreateGameWithTwentySixCases() {
        GameView game = gameService.createGame();

        assertThat(game.status()).isEqualTo(GameStatus.SELECTING_PERSONAL_CASE);
        assertThat(game.cases()).hasSize(26);
        assertThat(game.remainingAmounts()).hasSize(26);
    }

    @Test
    void shouldReachBankerOfferAfterFirstRoundOpenings() {
        GameView game = gameService.createGame();
        game = gameService.selectPersonalCase(game.id(), 1);

        for (int caseNumber = 2; caseNumber <= 7; caseNumber++) {
            game = gameService.openCase(game.id(), caseNumber);
        }

        assertThat(game.status()).isEqualTo(GameStatus.BANKER_OFFER);
        assertThat(game.currentOffer()).isNotNull();
        assertThat(game.openedAmounts()).hasSize(6);
        assertThat(game.remainingAmounts()).hasSize(20);
    }

    @Test
    void shouldRevealCasesAfterAcceptingDeal() {
        GameView game = gameService.createGame();
        game = gameService.selectPersonalCase(game.id(), 1);
        for (int caseNumber = 2; caseNumber <= 7; caseNumber++) {
            game = gameService.openCase(game.id(), caseNumber);
        }

        GameView finished = gameService.acceptDeal(game.id());

        assertThat(finished.status()).isEqualTo(GameStatus.DEAL_ACCEPTED);
        assertThat(finished.acceptedOffer()).isNotNull();
        assertThat(finished.cases()).allSatisfy(briefcase -> {
            assertThat(briefcase.opened()).isTrue();
            assertThat(briefcase.amount()).isNotNull();
        });
    }

    @Test
    void shouldNotOpenPersonalCaseEarly() {
        GameView game = gameService.createGame();
        game = gameService.selectPersonalCase(game.id(), 1);
        String gameId = game.id();

        assertThatThrownBy(() -> gameService.openCase(gameId, 1))
                .isInstanceOf(GameException.class)
                .hasMessageContaining("专属箱子不能提前打开");
    }
}
