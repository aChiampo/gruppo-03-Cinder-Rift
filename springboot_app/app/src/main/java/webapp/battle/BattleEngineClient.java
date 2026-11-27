package webapp.battle;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Calls the engine once, without user credentials or automatic POST retries. */
@Component
public class BattleEngineClient {
    public static final String DEADLINE_HEADER = "X-Battle-Deadline";
    private final HttpClient client;
    private final JsonMapper json;
    private final BattleResponseAdapter adapter;
    private final String baseUrl;

    public BattleEngineClient(JsonMapper json, BattleResponseAdapter adapter,
            @Value("${app.battle.engine-url:http://localhost:8083}") String baseUrl) {
        this.json = json;
        this.adapter = adapter;
        URI uri = URI.create(baseUrl);
        if (!SCHEMES.contains(uri.getScheme()) || uri.getHost() == null
                || uri.getRawQuery() != null || uri.getRawFragment() != null || uri.getUserInfo() != null)
            throw new IllegalArgumentException("Invalid battle engine URL");
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1))
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }

    private static final java.util.Set<String> SCHEMES = java.util.Set.of("http", "https");

    public BattleResponse execute(String stageId, EngineBattleRequest payload, BattleBudget budget) {
        // Encode the stage as one path segment; URI resolution must never select another host or route.
        String segment = java.net.URLEncoder.encode(stageId, java.nio.charset.StandardCharsets.UTF_8)
                .replace("+", "%20");
        JsonNode body = postJson("/api/battle/pve/" + segment, payload, budget, this::mapPveError);
        return adapter.adapt(body, payload.challenger());
    }

    /** Executes one PvP battle against the game engine without forwarding user credentials. */
    public EnginePvpBattleResponse executePvp(EnginePvpBattleRequest payload, BattleBudget budget) {
        JsonNode body = postJson("/api/battle/pvp", payload, budget, (status, errorBody) -> BattleFailure.integration());
        return adaptPvpResponse(body);
    }

    private JsonNode postJson(String path, Object payload, BattleBudget budget,
            BiFunction<Integer, JsonNode, BattleFailure> errorMapper) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(budget.remaining())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, application/problem+json")
                .header(DEADLINE_HEADER, budget.engineDeadline().toString())
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload))).build();
        var pending = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        try {
            HttpResponse<String> response = pending.get(budget.remaining().toNanos(), TimeUnit.NANOSECONDS);
            budget.remaining();
            String type = response.headers().firstValue("Content-Type").orElse("").split(";", 2)[0].trim();
            if (!(type.equalsIgnoreCase("application/json") || type.equalsIgnoreCase("application/problem+json")))
                throw BattleFailure.integration();
            JsonNode body;
            try { body = json.readTree(response.body()); }
            catch (RuntimeException malformed) { throw BattleFailure.integration(); }
            if (response.statusCode() == 200) return body;
            if (body == null || !body.isObject()) throw BattleFailure.integration();
            JsonNode status = body.get("status");
            if (status != null && (!status.isIntegralNumber() || !status.canConvertToInt()
                    || status.intValue() != response.statusCode()))
                throw BattleFailure.integration();
            switch (response.statusCode()) {
                case 504 -> throw BattleFailure.timeout();
                case 503 -> throw BattleFailure.unavailable();
                default -> throw errorMapper.apply(response.statusCode(), body);
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw BattleFailure.timeout();
        } catch (TimeoutException timeout) {
            throw BattleFailure.timeout();
        } catch (ExecutionException failure) {
            if (failure.getCause() instanceof HttpTimeoutException) throw BattleFailure.timeout();
            throw BattleFailure.unavailable();
        } finally {
            pending.cancel(true);
        }
    }

    private BattleFailure mapPveError(int statusCode, JsonNode body) {
        return switch (statusCode) {
            case 404 -> new BattleFailure(HttpStatus.NOT_FOUND, "Stage Not Found", "Stage not found.",
                    Map.of("stageId", BattleResponseAdapter.requiredText(body, "stageId")));
            case 409 -> new BattleFailure(HttpStatus.CONFLICT, "Inactive Stage Event",
                    "The special event required for this stage is inactive.",
                    Map.of("eventCode", BattleResponseAdapter.requiredText(body, "eventCode")));
            default -> BattleFailure.integration();
        };
    }

    private EnginePvpBattleResponse adaptPvpResponse(JsonNode body) {
        if (body == null || !body.isObject()) throw BattleFailure.integration();
        String mode = BattleResponseAdapter.requiredText(body, "mode");
        String outcome = BattleResponseAdapter.requiredText(body, "outcome");
        String winnerUserId = optionalText(body, "winnerUserId");
        String winnerUsername = optionalText(body, "winnerUsername");
        JsonNode logNode = body.get("battleLog");
        if (logNode == null || !logNode.isArray()) throw BattleFailure.integration();
        List<String> battleLog = new ArrayList<>(logNode.size());
        for (JsonNode entry : logNode) {
            if (entry == null || !entry.isString()) throw BattleFailure.integration();
            battleLog.add(entry.asString());
        }
        return new EnginePvpBattleResponse(mode, outcome, winnerUserId, winnerUsername, battleLog);
    }

    private static String optionalText(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null || node.isNull()) return null;
        return BattleResponseAdapter.requiredText(body, field);
    }
}
