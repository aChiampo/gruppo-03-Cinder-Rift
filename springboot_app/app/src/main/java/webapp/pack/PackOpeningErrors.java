package webapp.pack;

import java.io.IOException;
import java.util.LinkedHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/** Shares stable opening error codes with the security filters. */
public final class PackOpeningErrors {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private PackOpeningErrors() { }

    public static boolean matches(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals("/api/users/me/pack-openings") || path.equals("/api/users/me/inventory")
                || path.equals("/api/users/me/inventory/packs");
    }

    static ProblemDetail problem(PackOpeningFailure failure, HttpServletRequest request) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(failure.status()), failure.getMessage());
        problem.setType(java.net.URI.create("about:blank"));
        problem.setInstance(java.net.URI.create(request.getRequestURI()));
        problem.setProperty("code", failure.code());
        failure.properties().forEach(problem::setProperty);
        return problem;
    }

    public static void write(HttpServletRequest request, HttpServletResponse response, int status) throws IOException {
        var body = new LinkedHashMap<String, Object>();
        body.put("type", "about:blank");
        body.put("title", HttpStatus.valueOf(status).getReasonPhrase());
        body.put("status", status);
        body.put("detail", status == 401 ? "Authentication is required." : "The operation is not permitted.");
        body.put("instance", request.getRequestURI());
        body.put("code", status == 401 ? "UNAUTHENTICATED" : "FORBIDDEN");
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        if (status == 401) response.setHeader("WWW-Authenticate", "Bearer");
        response.getWriter().write(JSON.writeValueAsString(body));
    }
}
