package com.benedetto.game_engine.battle.controller;

import com.benedetto.game_engine.battle.exception.InactiveStageEventException;
import com.benedetto.game_engine.battle.exception.InvalidDeckException;
import com.benedetto.game_engine.battle.exception.StageNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class BattleExceptionHandler {

    @ExceptionHandler(InvalidDeckException.class)
    public ProblemDetail handleInvalidDeckException(InvalidDeckException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.valueOf(422),
                ex.getMessage()
        );
        problemDetail.setTitle("Invalid Deck");
        problemDetail.setProperty("violations", ex.getViolations());
        return problemDetail;
    }

    @ExceptionHandler(StageNotFoundException.class)
    public ProblemDetail handleStageNotFoundException(StageNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problemDetail.setTitle("Stage Not Found");
        if (ex.getStageId() != null) {
            problemDetail.setProperty("stageId", ex.getStageId());
        }
        return problemDetail;
    }

    @ExceptionHandler(InactiveStageEventException.class)
    public ProblemDetail handleInactiveStageEventException(InactiveStageEventException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problemDetail.setTitle("Inactive Stage Event");
        if (ex.getEventCode() != null) {
            problemDetail.setProperty("eventCode", ex.getEventCode());
        }
        return problemDetail;
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ProblemDetail handleBadRequestExceptions(Exception ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle("Bad Request");
        return problemDetail;
    }
}
