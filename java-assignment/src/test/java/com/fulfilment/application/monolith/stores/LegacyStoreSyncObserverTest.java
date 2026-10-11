package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LegacyStoreSyncObserverTest {

  private LegacyStoreSyncObserver observer;
  private LegacyStoreManagerGateway gateway;
  private Store store;

  @BeforeEach
  public void setUp() {
    gateway = mock(LegacyStoreManagerGateway.class);
    observer = new LegacyStoreSyncObserver();
    observer.legacyStoreManagerGateway = gateway;
    store = new Store("TEST");
    store.id = 1L;
  }

  @Test
  public void createdEventShouldCallCreateOnLegacySystem() {
    observer.onStoreChanged(new StoreChangedEvent(store, StoreChangedEvent.Type.CREATED));

    verify(gateway).createStoreOnLegacySystem(store);
    verifyNoMoreInteractions(gateway);
  }

  @Test
  public void updatedEventShouldCallUpdateOnLegacySystem() {
    observer.onStoreChanged(new StoreChangedEvent(store, StoreChangedEvent.Type.UPDATED));

    verify(gateway).updateStoreOnLegacySystem(store);
    verifyNoMoreInteractions(gateway);
  }

  @Test
  public void legacyFailureShouldBeLoggedAndNotPropagated() {
    doThrow(new RuntimeException("legacy down")).when(gateway).createStoreOnLegacySystem(store);

    assertDoesNotThrow(
        () ->
            observer.onStoreChanged(
                new StoreChangedEvent(store, StoreChangedEvent.Type.CREATED)));
  }
}
