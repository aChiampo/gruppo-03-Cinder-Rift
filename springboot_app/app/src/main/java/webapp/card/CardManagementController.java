package webapp.card;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

/** Administrative endpoints for card catalog management. */
@RestController
@RequestMapping("/api/admin/cards")
@Validated
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Card administration", description = "Admin-only card catalog management")
public class CardManagementController {
    private final CardManagementService cards;

    public CardManagementController(CardManagementService cards) {
        this.cards = cards;
    }

    @PostMapping
    @Operation(summary = "Create a card in the catalog")
    public ResponseEntity<CardCatalogItem> create(@Valid @RequestBody CardWriteRequest request) {
        CardCatalogItem created = cards.create(request);
        return ResponseEntity.created(URI.create("/api/cards/" + created.id())).body(created);
    }

    @PutMapping("/{cardId}")
    @Operation(summary = "Replace an unused card in the catalog")
    public CardCatalogItem replace(
            @PathVariable @Positive int cardId,
            @Valid @RequestBody CardWriteRequest request) {
        return cards.replace(cardId, request);
    }

    @DeleteMapping("/{cardId}")
    @Operation(summary = "Logically delete an unused card from the catalog")
    public ResponseEntity<Void> delete(@PathVariable @Positive int cardId) {
        cards.delete(cardId);
        return ResponseEntity.noContent().build();
    }
}
