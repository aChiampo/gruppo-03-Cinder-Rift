package webapp.pack;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import webapp.auth.UserProfile;

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
    @Operation(summary = "List visible package definitions in ID order without pagination")
    public List<PackView> list(@Parameter(hidden = true) @AuthenticationPrincipal UserProfile user) {
        return packs.list("ADMIN".equals(user.role()));
    }

    @GetMapping("/{packId}")
    @Operation(summary = "Get one package definition")
    public PackView get(@PathVariable @Positive int packId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserProfile user) {
        return packs.getVisible(packId, "ADMIN".equals(user.role()));
    }
}
