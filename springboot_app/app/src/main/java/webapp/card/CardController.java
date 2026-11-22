package webapp.card;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

/** Exposes the authenticated card catalog. */
@RestController
@RequestMapping("/api/cards")
@Validated
@Tag(name = "Cards", description = "Card catalog")
public class CardController {
    private final CardCatalogService cards;

    public CardController(CardCatalogService cards) {
        this.cards = cards;
    }

    @GetMapping
    @Operation(summary = "Search catalog cards not logically deleted and their artwork variants",
            security = @SecurityRequirement(name = "bearerAuth"))
    public CardPage<CardCatalogItem> search(@Valid @ModelAttribute CardSearchParameters parameters) {
        return cards.search(parameters.toCriteria());
    }

    @GetMapping("/{cardId}")
    @Operation(summary = "Get one active catalog card",
            security = @SecurityRequirement(name = "bearerAuth"))
    public CardCatalogItem getCard(@PathVariable @Positive int cardId) {
        return cards.findById(cardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));
    }
}
