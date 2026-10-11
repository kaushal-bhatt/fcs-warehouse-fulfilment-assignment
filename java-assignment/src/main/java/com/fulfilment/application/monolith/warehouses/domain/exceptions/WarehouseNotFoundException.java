package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/** No active warehouse matches the given identifier. */
public class WarehouseNotFoundException extends WarehouseDomainException {

  public WarehouseNotFoundException(String format, Object... args) {
    super(format, args);
  }
}
