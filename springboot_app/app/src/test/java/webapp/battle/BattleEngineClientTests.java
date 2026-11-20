package webapp.battle;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

class BattleEngineClientTests {
    private final JsonMapper json = JsonMapper.builder().build();
    private HttpServer server;
    private BattleEngineClient client;
    private final AtomicReference<String> received = new AtomicReference<>();
    private final AtomicReference<String> deadline = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private int responseStatus;
    private String responseType;
    private String responseBody;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/battle/pve/", exchange -> {
            received.set(new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            deadline.set(exchange.getRequestHeaders().getFirst(BattleEngineClient.DEADLINE_HEADER));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] bytes = responseBody.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", responseType);
            exchange.sendResponseHeaders(responseStatus, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        client = new BattleEngineClient(json, new BattleResponseAdapter(), "http://127.0.0.1:" + server.getAddress().getPort());
        responseType = "application/problem+json";
    }

    @AfterEach void stop() { server.stop(0); }

    private EngineBattleRequest payload() {
        return new EngineBattleRequest(new EngineBattleRequest.Challenger("42", "Player", "Player"),
                new EngineBattleRequest.Deck(List.of()));
    }

    @Test
    void sendsOnlyServerSnapshotAndAbsoluteDeadlineWithoutBearer() {
        responseStatus = 200;
        responseType = "application/json; charset=utf-8";
        responseBody = """
                {"stageId":"stage_01","stageNumber":1,"stageTitle":"Title",
                 "challenger":{"userId":"42","username":"Player","displayName":"Player"},
                 "enemyName":"Bot","outcome":"VICTORY","winner":"Player",
                 "tutorialHints":["hint"],"battleLog":["hint","combat"]}
                """;
        BattleBudget budget = BattleBudget.start(Clock.systemUTC());
        assertThat(client.execute("stage_01", payload(), budget).battleLog()).containsExactly("combat");
        assertThat(deadline.get()).isEqualTo(budget.engineDeadline().toString());
        assertThat(authorization.get()).isNull();
        assertThat(json.readTree(received.get()).get("challenger").get("userId").stringValue()).isEqualTo("42");
    }

    @ParameterizedTest
    @CsvSource({"400,502", "401,502", "422,502", "500,502", "504,504", "503,503"})
    void translatesInternalFailuresWithoutBlamingThePublicDeck(int internal, int expected) {
        responseStatus = internal;
        responseBody = "{\"status\":" + internal + ",\"detail\":\"internal secret\"}";
        BattleFailure failure = catchThrowableOfType(BattleFailure.class,
                () -> client.execute("stage_01", payload(), BattleBudget.start(Clock.systemUTC())));
        assertThat(failure.response("/test").getStatusCode().value()).isEqualTo(expected);
        assertThat(failure.getMessage()).doesNotContain("secret");
    }

    @Test
    void preservesOnlyApprovedErrorProperties() {
        responseStatus = 404;
        responseBody = "{\"status\":404,\"stageId\":\"missing\",\"secret\":\"hidden\"}";
        BattleFailure missing = catchThrowableOfType(BattleFailure.class,
                () -> client.execute("missing", payload(), BattleBudget.start(Clock.systemUTC())));
        assertThat(missing.response("/test").getBody().getProperties()).containsOnlyKeys("stageId");
        responseStatus = 409;
        responseBody = "{\"eventCode\":\"BLOOD_MOON\"}";
        BattleFailure inactive = catchThrowableOfType(BattleFailure.class,
                () -> client.execute("stage_03", payload(), BattleBudget.start(Clock.systemUTC())));
        assertThat(inactive.response("/test").getBody().getProperties()).containsEntry("eventCode", "BLOOD_MOON");
    }

    @Test
    void rejectsMalformedErrorsAndUnsupportedContentTypes() {
        responseStatus = 404;
        for (String body : List.of("not JSON", "null", "[]", "{}", "{\"status\":200,\"stageId\":\"x\"}")) {
            responseBody = body;
            BattleFailure failure = catchThrowableOfType(BattleFailure.class,
                    () -> client.execute("x", payload(), BattleBudget.start(Clock.systemUTC())));
            assertThat(failure.response("/test").getStatusCode().value()).isEqualTo(502);
        }
        responseBody = "{}";
        responseType = "text/html";
        assertThatThrownBy(() -> client.execute("x", payload(), BattleBudget.start(Clock.systemUTC())))
                .isInstanceOf(BattleFailure.class);
    }

    @Test
    void refusesExpiredBudgetBeforeSendingAnythingAndMapsConnectionRefusal() {
        BattleBudget expired = new BattleBudget(System.nanoTime() - Duration.ofSeconds(10).toNanos(), Instant.now());
        assertThatThrownBy(() -> client.execute("x", payload(), expired)).isInstanceOf(BattleFailure.class);
        assertThat(received.get()).isNull();
        server.stop(0);
        BattleFailure failure = catchThrowableOfType(BattleFailure.class,
                () -> client.execute("x", payload(), BattleBudget.start(Clock.systemUTC())));
        assertThat(failure.response("/test").getStatusCode().value()).isEqualTo(503);
    }
}
