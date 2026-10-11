package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.common.ErrorResponse;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseDomainException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/** Maps warehouse business rule violations to the status codes declared in the OpenAPI spec. */
@Provider
public class WarehouseExceptionMapper implements ExceptionMapper<WarehouseDomainException> {

  private static final Logger LOGGER = Logger.getLogger(WarehouseExceptionMapper.class);

  @Override
  public Response toResponse(WarehouseDomainException exception) {
    int status =
        exception instanceof WarehouseNotFoundException
            ? Response.Status.NOT_FOUND.getStatusCode()
            : Response.Status.BAD_REQUEST.getStatusCode();

    // expected client errors: no stack trace needed
    LOGGER.warnf("Warehouse request rejected (%d): %s", status, exception.getMessage());

    return ErrorResponse.of(status, exception.getMessage());
  }
}
