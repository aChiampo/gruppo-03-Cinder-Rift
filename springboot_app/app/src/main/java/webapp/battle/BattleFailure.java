package webapp.battle;

import java.net.URI;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;

/** Public battle failures contain only approved, caller-safe information. */
public class BattleFailure extends RuntimeException {
    private final HttpStatus status;
    private final String title;
    private final Map<String, Object> properties;

    public BattleFailure(HttpStatus status, String title, String detail) {
        this(status, title, detail, Map.of());
    }

    public BattleFailure(HttpStatus status, String title, String detail, Map<String, Object> properties) {
        super(detail);
        this.status = status;
        this.title = title;
        this.properties = Map.copyOf(properties);
    }

    public ResponseEntity<ProblemDetail> response(String path) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, getMessage());
        problem.setTitle(title);
        problem.setInstance(URI.create(path));
        properties.forEach(problem::setProperty);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem);
    }

    public static BattleFailure timeout() {
        return new BattleFailure(HttpStatus.GATEWAY_TIMEOUT, "Gateway Timeout", "The battle deadline was exceeded.");
    }

    public static BattleFailure integration() {
        return new BattleFailure(HttpStatus.BAD_GATEWAY, "Bad Gateway", "The battle engine returned an invalid response.");
    }

    public static BattleFailure unavailable() {
        return new BattleFailure(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", "The battle engine is unavailable.");
    }
}
