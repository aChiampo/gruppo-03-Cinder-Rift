package webapp.battle;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import static org.assertj.core.api.Assertions.*;

class BattleResponseAdapterTests {
    private final JsonMapper json = JsonMapper.builder().build();
    private final BattleResponseAdapter adapter = new BattleResponseAdapter();
    private final EngineBattleRequest.Challenger user = new EngineBattleRequest.Challenger("42", "Player", "Player");

    private ObjectNode response() {
        return (ObjectNode) json.readTree("""
                {"stageId":"stage_01","stageNumber":1,"stageTitle":"Tutorial: title",
                 "challenger":{"userId":"42","username":"Player","displayName":"Player"},
                 "enemyName":"Player","outcome":"DEFEAT","winner":"Player",
                 "tutorialHints":["hint"],"battleLog":["hint","  line\\nnext  ","hint","[EFFECT] event"]}
                """);
    }

    @Test
    void removesOnlyTheVerifiedPrefixAndPreservesAllOtherText() {
        BattleResponse result = adapter.adapt(response(), user);
        assertThat(result.outcome()).isEqualTo("DEFEAT");
        assertThat(result.stageTitle()).isEqualTo("Tutorial: title");
        assertThat(result.battleLog()).containsExactly("  line\nnext  ", "hint", "[EFFECT] event");
        assertThat(json.readTree(json.writeValueAsString(result)).has("tutorialHints")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"stageId", "stageNumber", "stageTitle", "challenger", "enemyName",
            "outcome", "winner", "tutorialHints", "battleLog"})
    void rejectsMissingInternalFields(String field) {
        ObjectNode root = response();
        root.remove(field);
        assertThatThrownBy(() -> adapter.adapt(root, user)).isInstanceOf(BattleFailure.class);
    }

    @Test
    void rejectsBadTutorialPrefixesTypesOutcomesAndIdentities() {
        for (String patch : List.of("{\"battleLog\":[\"wrong\",\"hint\"]}",
                "{\"battleLog\":[\"hint\",null]}", "{\"tutorialHints\":null}",
                "{\"outcome\":\"UNKNOWN\"}", "{\"stageNumber\":1.5}",
                "{\"winner\":\"NONE\"}", "{\"challenger\":{\"userId\":\"99\"}}")) {
            ObjectNode root = response();
            root.setAll((ObjectNode) json.readTree(patch));
            assertThatThrownBy(() -> adapter.adapt(root, user)).isInstanceOf(BattleFailure.class);
        }
    }

    @Test
    void acceptsDrawFromStructuredOutcomeEvenWhenNamesAreEqual() {
        ObjectNode root = response();
        root.put("outcome", "DRAW");
        root.put("winner", "NONE");
        assertThat(adapter.adapt(root, user).outcome()).isEqualTo("DRAW");
    }
}
