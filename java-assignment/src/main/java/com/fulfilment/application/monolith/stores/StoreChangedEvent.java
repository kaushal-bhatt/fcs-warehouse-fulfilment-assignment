package com.fulfilment.application.monolith.stores;

/** Fired inside the store transaction; observed only after a successful commit. */
public record StoreChangedEvent(Store store, Type type) {

  public enum Type {
    CREATED,
    UPDATED
  }
}
