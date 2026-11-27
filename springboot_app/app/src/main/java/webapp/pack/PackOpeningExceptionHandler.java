package webapp.pack;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.servlet.http.HttpServletRequest;

/** Does not expose SQL or partial results when a batch fails. */
@RestControllerAdvice(assignableTypes = PackOpeningController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PackOpeningExceptionHandler {
    @ExceptionHandler(PackOpeningFailure.class)
    ResponseEntity<ProblemDetail> expected(PackOpeningFailure failure, HttpServletRequest request) {
        return ResponseEntity.status(failure.status()).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(PackOpeningErrors.problem(failure, request));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> malformed(HttpServletRequest request) {
        return expected(new PackOpeningFailure(400, "INVALID_REQUEST", "The request has an invalid representation."), request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ProblemDetail> media(HttpServletRequest request) {
        return expected(new PackOpeningFailure(415, "UNSUPPORTED_MEDIA_TYPE", "Use application/json."), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> denied(HttpServletRequest request) {
        return expected(new PackOpeningFailure(403, "FORBIDDEN", "The operation is not permitted."), request);
    }

    @ExceptionHandler({TransientDataAccessException.class, DataAccessResourceFailureException.class})
    ResponseEntity<ProblemDetail> unavailable(HttpServletRequest request) {
        return expected(new PackOpeningFailure(503, "TEMPORARILY_UNAVAILABLE", "The operation is temporarily unavailable."), request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> unexpected(HttpServletRequest request) {
        return expected(new PackOpeningFailure(500, "INTERNAL_ERROR", "The opening could not be completed."), request);
    }
}
