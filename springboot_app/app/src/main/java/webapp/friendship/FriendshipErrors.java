package webapp.friendship;

import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/** Shares the F13 error envelope between MVC and security-filter failures. */
public final class FriendshipErrors {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private FriendshipErrors() { }

    public static boolean matches(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals("/api/friend-requests") || path.startsWith("/api/friend-requests/")
                || path.equals("/api/me/friend-requests") || path.equals("/api/me/friends")
                || path.startsWith("/api/me/friends/") || path.matches("/api/users/[^/]+/friendship");
    }

    static FriendshipError problem(int status, String code, String detail, HttpServletRequest request) {
        return new FriendshipError("about:blank", HttpStatus.valueOf(status).getReasonPhrase(),
                status, detail, request.getRequestURI(), code);
    }

    public static void write(HttpServletRequest request, HttpServletResponse response, int status) throws IOException {
        String code = status == 401 ? "UNAUTHORIZED" : "FORBIDDEN";
        String detail = status == 401 ? "Authentication is required." : "The operation is not permitted.";
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        if (status == 401) response.setHeader("WWW-Authenticate", "Bearer");
        response.getWriter().write(JSON.writeValueAsString(problem(status, code, detail, request)));
    }
}
