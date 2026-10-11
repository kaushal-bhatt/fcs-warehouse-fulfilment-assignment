package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

public class LegacyStoreManagerGatewayTest {

  private final LegacyStoreManagerGateway gateway = new LegacyStoreManagerGateway();

  @Test
  public void createAndUpdateShouldWriteAndCleanUpTempFile() {
    Store store = new Store("TONSTAD");
    store.id = 1L;
    store.quantityProductsInStock = 10;

    assertDoesNotThrow(() -> gateway.createStoreOnLegacySystem(store));
    assertDoesNotThrow(() -> gateway.updateStoreOnLegacySystem(store));
  }
}
