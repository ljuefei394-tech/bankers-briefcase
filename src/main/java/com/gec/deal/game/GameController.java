package com.gec.deal.game;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping
    public GameView createGame() {
        return gameService.createGame();
    }

    @GetMapping("/{gameId}")
    public GameView getGame(@PathVariable String gameId) {
        return gameService.getGame(gameId);
    }

    @PostMapping("/{gameId}/select/{caseNumber}")
    public GameView selectPersonalCase(@PathVariable String gameId, @PathVariable int caseNumber) {
        return gameService.selectPersonalCase(gameId, caseNumber);
    }

    @PostMapping("/{gameId}/open/{caseNumber}")
    public GameView openCase(@PathVariable String gameId, @PathVariable int caseNumber) {
        return gameService.openCase(gameId, caseNumber);
    }

    @PostMapping("/{gameId}/deal")
    public GameView acceptDeal(@PathVariable String gameId) {
        return gameService.acceptDeal(gameId);
    }

    @PostMapping("/{gameId}/no-deal")
    public GameView rejectDeal(@PathVariable String gameId) {
        return gameService.rejectDeal(gameId);
    }

    @PostMapping("/{gameId}/keep")
    public GameView keepPersonalCase(@PathVariable String gameId) {
        return gameService.keepPersonalCase(gameId);
    }

    @PostMapping("/{gameId}/switch")
    public GameView switchToLastCase(@PathVariable String gameId) {
        return gameService.switchToLastCase(gameId);
    }

    @ExceptionHandler(GameException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleGameException(GameException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    public record ErrorResponse(String message) {
    }
}
