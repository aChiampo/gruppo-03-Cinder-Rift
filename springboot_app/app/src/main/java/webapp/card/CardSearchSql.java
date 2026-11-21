package webapp.card;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Builds parameterized predicates and allow-listed ordering for card read queries. */
public final class CardSearchSql {
    public static final String REFERENCE_JOINS = """
            LEFT JOIN factions f ON f.id = c.id_faction AND NOT f.is_deleted
            LEFT JOIN rarities r ON r.id = c.id_rarity AND NOT r.is_deleted
            LEFT JOIN card_types t ON t.id = c.id_type AND NOT t.is_deleted
            LEFT JOIN expansions e ON e.id = c.id_expansion AND NOT e.is_deleted
            """;

    public static final String FROM_AND_JOINS = """
            FROM cards c
            """ + REFERENCE_JOINS;

    private CardSearchSql() {
    }

    public static QueryParts build(CardSearchCriteria criteria, Integer ownerId) {
        List<String> predicates = new ArrayList<>();
        Map<String, Object> parameters = new LinkedHashMap<>();
        predicates.add("c.is_deleted = false");
        if (ownerId != null) {
            predicates.add("co.id_user = :ownerId");
            parameters.put("ownerId", ownerId);
        }
        addLike(predicates, parameters, "name", "LOWER(c.name)", criteria.name());
        addCode(predicates, parameters, "f.code", "faction", criteria.faction());
        addCode(predicates, parameters, "r.code", "rarity", criteria.rarity());
        addCode(predicates, parameters, "t.code", "type", criteria.type());
        addCode(predicates, parameters, "e.code", "expansion", criteria.expansion());
        addRange(predicates, parameters, "c.energy_cost", "energyMin", criteria.energyMin(),
                "energyMax", criteria.energyMax());
        addRange(predicates, parameters, "c.value_atk", "attackMin", criteria.attackMin(),
                "attackMax", criteria.attackMax());
        addRange(predicates, parameters, "c.value_def", "defenseMin", criteria.defenseMin(),
                "defenseMax", criteria.defenseMax());
        addRange(predicates, parameters, "c.power", "powerMin", criteria.powerMin(),
                "powerMax", criteria.powerMax());
        return new QueryParts(
                " WHERE " + String.join(" AND ", predicates),
                Map.copyOf(parameters),
                orderBy(criteria.sort(), criteria.direction(), ownerId != null));
    }

    private static void addLike(List<String> predicates, Map<String, Object> parameters,
            String parameter, String column, String value) {
        if (value != null) {
            predicates.add(column + " LIKE :" + parameter);
            parameters.put(parameter, "%" + value.toLowerCase(Locale.ROOT) + "%");
        }
    }

    private static void addCode(List<String> predicates, Map<String, Object> parameters,
            String column, String parameter, String value) {
        if (value != null) {
            predicates.add("UPPER(" + column + ") = :" + parameter);
            parameters.put(parameter, value);
        }
    }

    private static void addRange(List<String> predicates, Map<String, Object> parameters,
            String column, String minimumName, Integer minimum, String maximumName, Integer maximum) {
        if (minimum != null) {
            predicates.add(column + " >= :" + minimumName);
            parameters.put(minimumName, minimum);
        }
        if (maximum != null) {
            predicates.add(column + " <= :" + maximumName);
            parameters.put(maximumName, maximum);
        }
    }

    private static String orderBy(String sort, String direction, boolean collection) {
        String column = switch (sort) {
            case "name" -> "LOWER(c.name)";
            case "faction" -> "LOWER(f.name)";
            case "rarity" -> "r.order_index";
            case "energy" -> "c.energy_cost";
            case "attack" -> "c.value_atk";
            case "defense" -> "c.value_def";
            case "power" -> "c.power";
            default -> throw new IllegalArgumentException("Unvalidated card sort field");
        };
        String tieBreaker = collection ? ", co.id_card_art ASC" : "";
        return column + " " + direction.toUpperCase(Locale.ROOT) + " NULLS LAST, c.id ASC" + tieBreaker;
    }

    public record QueryParts(String where, Map<String, Object> parameters, String orderBy) {
    }
}
