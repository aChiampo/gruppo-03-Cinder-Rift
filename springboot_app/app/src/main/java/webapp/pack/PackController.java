package webapp.pack;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import webapp.card.CardPage;

/** Authenticated package catalog, shared by USER and ADMIN. */
@RestController
@RequestMapping("/api/packs")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Packs", description = "Package configuration catalog")
public class PackController {
    private final PackService packs;

    public PackController(PackService packs) {
        this.packs = packs;
    }

    @GetMapping
    @Operation(summary = "List package definitions in ID order")
    public CardPage<PackView> list(@RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return packs.list(page, size);
    }

    @GetMapping("/{packId}")
    @Operation(summary = "Get one package definition")
    public PackView get(@PathVariable @Positive int packId) {
        return packs.get(packId);
    }
}
