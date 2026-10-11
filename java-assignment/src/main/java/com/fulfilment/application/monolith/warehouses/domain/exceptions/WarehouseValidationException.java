package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/** The request breaks a warehouse business rule (invalid input, location limits, capacity...). */
public class WarehouseValidationException extends WarehouseDomainException {

  public WarehouseValidationException(String format, Object... args) {
    super(format, args);
  }
}
