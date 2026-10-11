package com.fulfilment.application.monolith.common;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** Error body shared by all endpoints. */
public record ErrorResponse(int code, String error) {

  public static Response of(int status, String message) {
    return Response.status(status)
        .type(MediaType.APPLICATION_JSON)
        .entity(new ErrorResponse(status, message))
        .build();
  }
}
