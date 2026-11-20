package webapp.battle;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/** Verifies the internal contract before exposing any engine data to the browser. */
@Component
public class BattleResponseAdapter {
    public BattleResponse adapt(JsonNode root, EngineBattleRequest.Challenger challenger) {
        if (root == null || !root.isObject()) throw BattleFailure.integration();
        String stageId = requiredText(root, "stageId");
        JsonNode number = root.get("stageNumber");
        if (number == null || !number.isIntegralNumber() || !number.canConvertToInt() || number.intValue() < 1)
            throw BattleFailure.integration();
        String title = requiredText(root, "stageTitle");
        String enemy = requiredText(root, "enemyName");
        String outcome = requiredText(root, "outcome");
        String winner = requiredText(root, "winner");
        if (!Set.of("VICTORY", "DEFEAT", "DRAW").contains(outcome)) throw BattleFailure.integration();
        JsonNode metadata = root.get("challenger");
        if (metadata == null || !challenger.userId().equals(requiredText(metadata, "userId"))
                || !challenger.username().equals(requiredText(metadata, "username"))
                || !challenger.displayName().equals(requiredText(metadata, "displayName")))
            throw BattleFailure.integration();
        String expectedWinner = switch (outcome) {
            case "VICTORY" -> challenger.displayName().trim();
            case "DEFEAT" -> enemy;
            default -> "NONE";
        };
        if (!expectedWinner.equals(winner)) throw BattleFailure.integration();
        List<String> hints = strings(root.get("tutorialHints"));
        List<String> log = strings(root.get("battleLog"));
        if (log.size() < hints.size() || !log.subList(0, hints.size()).equals(hints))
            throw BattleFailure.integration();
        return new BattleResponse(stageId, number.intValue(), title, challenger, enemy, outcome,
                winner, log.subList(hints.size(), log.size()));
    }

    static String requiredText(JsonNode root, String key) {
        JsonNode value = root.get(key);
        if (value == null || !value.isString() || value.stringValue().isBlank()) throw BattleFailure.integration();
        return value.stringValue();
    }

    private static List<String> strings(JsonNode array) {
        if (array == null || !array.isArray()) throw BattleFailure.integration();
        List<String> strings = new ArrayList<>();
        for (JsonNode item : array) {
            if (!item.isString()) throw BattleFailure.integration();
            strings.add(item.stringValue());
        }
        return strings;
    }
}
