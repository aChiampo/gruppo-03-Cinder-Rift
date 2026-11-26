package webapp.pack;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

/** Administrative CRUD for package definitions, without awarding packages to a player. */
@RestController
@RequestMapping("/api/admin/packs")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Pack administration", description = "Admin-only package configuration")
public class PackManagementController {
    private final PackService packs;

    public PackManagementController(PackService packs) {
        this.packs = packs;
    }

    @PostMapping
    @Operation(summary = "Create a package definition")
    public ResponseEntity<PackView> create(@Valid @RequestBody PackWriteRequest request) {
        PackView result = packs.create(request);
        return ResponseEntity.created(URI.create("/api/packs/" + result.id())).body(result);
    }

    @PutMapping("/{packId}")
    @Operation(summary = "Replace a package definition, preserving ownership and clearing mercy progress")
    public PackView replace(@PathVariable @Positive int packId, @Valid @RequestBody PackWriteRequest request) {
        return packs.replace(packId, request);
    }

    @DeleteMapping("/{packId}")
    @Operation(summary = "Delete a package definition, refund owned quantities and remove mercy progress")
    public ResponseEntity<Void> delete(@PathVariable @Positive int packId) {
        packs.delete(packId);
        return ResponseEntity.noContent().build();
    }
}
