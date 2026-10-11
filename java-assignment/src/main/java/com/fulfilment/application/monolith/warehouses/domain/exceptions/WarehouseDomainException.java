package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/**
 * Base type for business rule violations in the warehouse domain. Kept free of HTTP concerns; the
 * REST adapter decides how each subtype is exposed.
 */
public abstract class WarehouseDomainException extends RuntimeException {

  protected WarehouseDomainException(String format, Object... args) {
    super(String.format(format, args));
  }
}
