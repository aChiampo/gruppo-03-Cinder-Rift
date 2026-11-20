package com.benedetto.game_engine.battle.exception;

import java.util.Collections;
import java.util.List;

public class InvalidDeckException extends RuntimeException {

    private final List<String> violations;

    public InvalidDeckException(List<String> violations) {
        super(formatMessage(violations));
        this.violations = violations != null ? List.copyOf(violations) : Collections.emptyList();
    }

    public InvalidDeckException(String message, List<String> violations) {
        super(message);
        this.violations = violations != null ? List.copyOf(violations) : Collections.emptyList();
    }

    public List<String> getViolations() {
        return violations;
    }

    private static String formatMessage(List<String> violations) {
        if (violations == null || violations.isEmpty()) {
            return "Invalid challenger deck: no violations specified";
        }
        return "Invalid challenger deck: " + String.join("; ", violations);
    }
}
