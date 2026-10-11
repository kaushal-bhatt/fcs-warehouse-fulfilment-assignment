package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseDomainException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;
import org.jboss.logging.Logger;

/** Maps warehouse business rule violations to the status codes declared in the OpenAPI spec. */
@Provider
public class WarehouseExceptionMapper implements ExceptionMapper<WarehouseDomainException> {

  private static final Logger LOGGER = Logger.getLogger(WarehouseExceptionMapper.class);

  @Override
  public Response toResponse(WarehouseDomainException exception) {
    Response.Status status =
        exception instanceof WarehouseNotFoundException
            ? Response.Status.NOT_FOUND
            : Response.Status.BAD_REQUEST;

    // expected client errors: no stack trace needed
    LOGGER.warnf("Warehouse request rejected (%d): %s", status.getStatusCode(), exception.getMessage());

    return Response.status(status)
        .type(MediaType.APPLICATION_JSON)
        .entity(Map.of("code", status.getStatusCode(), "error", exception.getMessage()))
        .build();
  }
}
