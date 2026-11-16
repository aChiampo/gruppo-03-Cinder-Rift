package webapp.card;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

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
}
