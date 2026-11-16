package webapp.deck;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Carries rule violations to the RFC 7807 response adapter. */
final class DeckValidationException extends ResponseStatusException {
    private final DeckValidationResult validation;

    DeckValidationException(DeckValidationResult validation) {
        super(HttpStatus.BAD_REQUEST, "Deck validation failed");
        this.validation = validation;
    }

    DeckValidationResult validation() {
        return validation;
    }
}
