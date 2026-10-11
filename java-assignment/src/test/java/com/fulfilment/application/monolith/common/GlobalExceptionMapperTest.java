package com.fulfilment.application.monolith.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

public class GlobalExceptionMapperTest {

  private final GlobalExceptionMapper mapper = new GlobalExceptionMapper();

  @Test
  public void clientErrorShouldKeepStatusAndMessage() {
    Response response = mapper.toResponse(new WebApplicationException("Store not found", 404));

    assertEquals(new ErrorResponse(404, "Store not found"), response.getEntity());
  }

  @Test
  public void serverWebExceptionShouldKeepStatus() {
    Response response = mapper.toResponse(new WebApplicationException("Upstream failed", 502));

    assertEquals(502, response.getStatus());
  }

  @Test
  public void nestedUniqueViolationShouldBecomeConflict() {
    var sqlException = new SQLException("duplicate key value", "23505");
    var wrapped = new RuntimeException("commit failed", new PersistenceException(sqlException));

    Response response = mapper.toResponse(wrapped);

    assertEquals(
        new ErrorResponse(409, "A record with the same unique value already exists"),
        response.getEntity());
  }

  @Test
  public void otherSqlErrorShouldNotBecomeConflict() {
    var wrapped = new RuntimeException(new SQLException("connection lost", "08006"));

    assertEquals(500, mapper.toResponse(wrapped).getStatus());
  }

  @Test
  public void unexpectedErrorShouldHideInternalDetails() {
    Response response = mapper.toResponse(new IllegalStateException("secret internal detail"));

    assertEquals(
        new ErrorResponse(500, "Unexpected error, please try again later"), response.getEntity());
  }
}
