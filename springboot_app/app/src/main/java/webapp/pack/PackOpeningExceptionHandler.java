package webapp.pack;

import java.sql.SQLException;
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
import webapp.economy.EconomyFailure;

/** Does not expose SQL or partial results when a batch fails. */
@RestControllerAdvice(assignableTypes = {PackOpeningController.class, PackPurchaseController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PackOpeningExceptionHandler {
    @ExceptionHandler(EconomyFailure.class)
    ResponseEntity<ProblemDetail> economy(EconomyFailure failure, HttpServletRequest request) {
        return expected(new PackOpeningFailure(failure.status(), failure.code(), failure.getMessage(), failure.properties()), request);
    }

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
        return expected(new PackOpeningFailure(500, "INTERNAL_ERROR", "The operation could not be completed."), request);
    }

    // JDBC does not categorize PostgreSQL NOWAIT's 55P03 as a transient exception.
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    ResponseEntity<ProblemDetail> database(org.springframework.dao.DataAccessException failure, HttpServletRequest request) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && "55P03".equals(sql.getSQLState())) return unavailable(request);
        }
        return unexpected(request);
    }
}
