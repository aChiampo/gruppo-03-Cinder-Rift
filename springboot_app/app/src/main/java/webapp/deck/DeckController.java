package webapp.deck;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import webapp.auth.UserProfile;

/** Exposes ownership-scoped deck operations over the authenticated user's bearer session. */
@RestController
@RequestMapping("/api")
@Validated
@Tag(name = "Decks", description = "Deck management and live validation")
public class DeckController {
    private final DeckService decks;

    public DeckController(DeckService decks) {
        this.decks = decks;
    }

    @GetMapping("/users/me/decks")
    @Operation(summary = "List the authenticated user's decks",
            security = @SecurityRequirement(name = "bearerAuth"))
    public List<DeckListItemResponse> listDecks(
            @Parameter(hidden = true) @AuthenticationPrincipal UserProfile user) {
        return decks.listDeckViews(user.id()).stream().map(DeckListItemResponse::from).toList();
    }

    @PostMapping("/users/me/decks")
    @Operation(summary = "Create a deck with optional initial card copies",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<DeckDetailsResponse> createDeck(
            @Parameter(hidden = true) @AuthenticationPrincipal UserProfile user,
            @Valid @RequestBody(required = false) CreateDeckRequest request) {
        List<DeckEntry> entries = request == null || request.entries() == null
                ? List.of()
                : request.entries().stream().map(DeckEntryRequest::toDeckEntry).toList();
        DeckView created = decks.createDeck(user.id(), entries);
        return ResponseEntity.status(HttpStatus.CREATED).body(DeckDetailsResponse.from(created));
    }

    @GetMapping("/decks/{deckId}")
    @Operation(summary = "Get a deck and its live validation",
            security = @SecurityRequirement(name = "bearerAuth"))
    public DeckDetailsResponse getDeck(
            @Parameter(hidden = true) @AuthenticationPrincipal UserProfile user,
            @PathVariable @Positive int deckId) {
        return DeckDetailsResponse.from(decks.getDeckView(user.id(), deckId));
    }

    @PutMapping("/decks/{deckId}")
    @Operation(summary = "Replace deck entries and challengeability atomically",
            security = @SecurityRequirement(name = "bearerAuth"))
    public DeckDetailsResponse updateDeck(
            @Parameter(hidden = true) @AuthenticationPrincipal UserProfile user,
            @PathVariable @Positive int deckId,
            @Valid @RequestBody UpdateDeckRequest request) {
        List<DeckEntry> entries = request.entries().stream().map(DeckEntryRequest::toDeckEntry).toList();
        return DeckDetailsResponse.from(
                decks.updateDeck(user.id(), deckId, entries, request.challengeable()));
    }

    @DeleteMapping("/decks/{deckId}")
    @Operation(summary = "Delete an owned deck",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> deleteDeck(
            @Parameter(hidden = true) @AuthenticationPrincipal UserProfile user,
            @PathVariable @Positive int deckId) {
        decks.deleteDeck(user.id(), deckId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(DeckValidationException.class)
    public ResponseEntity<ProblemDetail> deckValidationProblem(
            DeckValidationException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getReason());
        problem.setTitle("Deck validation failed");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errors", exception.validation().errors());
        problem.setProperty("warnings", exception.validation().warnings());
        problem.setProperty("summary", exception.validation().summary());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE)
                .body(problem);
    }
}
