package com.fulfilment.application.monolith.stores;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Propagates store changes to the legacy system only once they are committed to our database. If
 * the transaction rolls back, this observer is never invoked.
 */
@ApplicationScoped
public class LegacyStoreSyncObserver {

  private static final Logger LOGGER = Logger.getLogger(LegacyStoreSyncObserver.class);

  @Inject LegacyStoreManagerGateway legacyStoreManagerGateway;

  void onStoreChanged(@Observes(during = TransactionPhase.AFTER_SUCCESS) StoreChangedEvent event) {
    Store store = event.store();
    try {
      switch (event.type()) {
        case CREATED -> legacyStoreManagerGateway.createStoreOnLegacySystem(store);
        case UPDATED -> legacyStoreManagerGateway.updateStoreOnLegacySystem(store);
      }
    } catch (RuntimeException e) {
      // The change is already committed and cannot be rolled back, so we only report the drift.
      // A production setup would use an outbox table with retries instead.
      LOGGER.errorf(e, "Store %d committed but legacy sync (%s) failed", store.id, event.type());
    }
  }
}
