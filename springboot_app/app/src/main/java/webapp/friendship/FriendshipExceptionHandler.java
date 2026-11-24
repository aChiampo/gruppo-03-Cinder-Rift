package webapp.friendship;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.servlet.http.HttpServletRequest;

/** Keeps syntax errors and unexpected infrastructure failures within the F13 contract. */
@RestControllerAdvice(assignableTypes = FriendshipController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FriendshipExceptionHandler {
    @ExceptionHandler(FriendshipFailure.class)
    ResponseEntity<FriendshipError> expected(FriendshipFailure failure, HttpServletRequest request) {
        return response(failure.status(), failure.code(), failure.getMessage(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<FriendshipError> malformed(HttpServletRequest request) {
        return response(400, "INVALID_REQUEST", "The request has an invalid representation.", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<FriendshipError> mediaType(HttpServletRequest request) {
        return response(415, "UNSUPPORTED_MEDIA_TYPE", "Use application/json for the request body.", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<FriendshipError> unexpected(HttpServletRequest request) {
        return response(500, "INTERNAL_ERROR", "The request could not be completed.", request);
    }

    private static ResponseEntity<FriendshipError> response(int status, String code, String detail,
            HttpServletRequest request) {
        var response = ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON);
        if (status == 401) response.header("WWW-Authenticate", "Bearer");
        return response.body(FriendshipErrors.problem(status, code, detail, request));
    }
}
