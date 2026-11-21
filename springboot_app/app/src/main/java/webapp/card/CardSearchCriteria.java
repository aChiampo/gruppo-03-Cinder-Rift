package webapp.card;

import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Normalized filters and ordering shared by catalog and collection reads. */
public record CardSearchCriteria(
        String name,
        String faction,
        String rarity,
        String type,
        String expansion,
        Integer energyMin,
        Integer energyMax,
        Integer attackMin,
        Integer attackMax,
        Integer defenseMin,
        Integer defenseMax,
        Integer powerMin,
        Integer powerMax,
        String sort,
        String direction,
        int page,
        int size) {

    private static final Set<String> SORT_FIELDS =
            Set.of("name", "faction", "rarity", "energy", "attack", "defense", "power");

    public static CardSearchCriteria create(
            String name,
            String faction,
            String rarity,
            String type,
            String expansion,
            Integer energyMin,
            Integer energyMax,
            Integer attackMin,
            Integer attackMax,
            Integer defenseMin,
            Integer defenseMax,
            Integer powerMin,
            Integer powerMax,
            String sort,
            String direction,
            int page,
            int size) {
        String normalizedSort = normalize(sort);
        String normalizedDirection = normalize(direction);
        if (normalizedSort == null || normalizedDirection == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sort and direction must not be blank");
        }
        normalizedSort = normalizedSort.toLowerCase(Locale.ROOT);
        normalizedDirection = normalizedDirection.toLowerCase(Locale.ROOT);
        if (!SORT_FIELDS.contains(normalizedSort)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported card sort field");
        }
        if (!normalizedDirection.equals("asc") && !normalizedDirection.equals("desc")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sort direction");
        }
        requireRange("energy", energyMin, energyMax);
        requireRange("attack", attackMin, attackMax);
        requireRange("defense", defenseMin, defenseMax);
        requireRange("power", powerMin, powerMax);

        return new CardSearchCriteria(
                normalize(name),
                normalizeCode(faction),
                normalizeCode(rarity),
                normalizeCode(type),
                normalizeCode(expansion),
                energyMin,
                energyMax,
                attackMin,
                attackMax,
                defenseMin,
                defenseMax,
                powerMin,
                powerMax,
                normalizedSort,
                normalizedDirection,
                page,
                size);
    }

    private static void requireRange(String field, Integer minimum, Integer maximum) {
        if (minimum != null && maximum != null && minimum > maximum) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, field + " minimum must not exceed maximum");
        }
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String normalizeCode(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }
}
