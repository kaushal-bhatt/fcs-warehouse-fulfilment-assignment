package com.fulfilment.application.monolith.common;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.sql.SQLException;
import org.jboss.logging.Logger;

/**
 * Fallback for every exception without a more specific mapper. Client errors keep their status
 * and message; anything unexpected becomes a 500 without exposing internals.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

  private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class);

  // SQLSTATE for a unique constraint violation
  private static final String UNIQUE_VIOLATION = "23505";
  private static final int MAX_CAUSE_DEPTH = 20;

  @Override
  public Response toResponse(Exception exception) {
    if (exception instanceof WebApplicationException webException) {
      int status = webException.getResponse().getStatus();
      if (status >= 500) {
        LOGGER.error("Request failed", exception);
      } else {
        LOGGER.warnf("Request rejected (%d): %s", status, exception.getMessage());
      }
      return ErrorResponse.of(status, exception.getMessage());
    }

    // usually raised on commit, wrapped in transaction exceptions
    if (isUniqueViolation(exception)) {
      LOGGER.warnf("Request rejected (409): %s", rootCause(exception).getMessage());
      return ErrorResponse.of(409, "A record with the same unique value already exists");
    }

    LOGGER.error("Unexpected error while handling request", exception);
    return ErrorResponse.of(500, "Unexpected error, please try again later");
  }

  private static boolean isUniqueViolation(Throwable exception) {
    Throwable current = exception;
    for (int depth = 0; current != null && depth < MAX_CAUSE_DEPTH; depth++) {
      if (current instanceof SQLException sql && UNIQUE_VIOLATION.equals(sql.getSQLState())) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  private static Throwable rootCause(Throwable exception) {
    Throwable current = exception;
    for (int depth = 0; current.getCause() != null && depth < MAX_CAUSE_DEPTH; depth++) {
      current = current.getCause();
    }
    return current;
  }
}
