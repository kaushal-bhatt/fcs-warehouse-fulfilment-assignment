package com.fulfilment.application.monolith.stores;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jboss.logging.Logger;

@ApplicationScoped
public class LegacyStoreManagerGateway {

  private static final Logger LOGGER = Logger.getLogger(LegacyStoreManagerGateway.class);

  public void createStoreOnLegacySystem(Store store) {
    // just to emulate as this would send this to a legacy system, let's write a temp file with the
    writeToFile(store, "created");
  }

  public void updateStoreOnLegacySystem(Store store) {
    // just to emulate as this would send this to a legacy system, let's write a temp file with the
    writeToFile(store, "updated");
  }

  private void writeToFile(Store store, String action) {
    String content =
        "Store "
            + action
            + ". [ id ="
            + store.id
            + " ] [ name ="
            + store.name
            + " ] [ items on stock ="
            + store.quantityProductsInStock
            + "]";
    try {
      Path tempFile = Files.createTempFile("store-" + store.id + "-", ".txt");
      Files.writeString(tempFile, content, StandardCharsets.UTF_8);
      LOGGER.infof("Legacy system notified: %s", Files.readString(tempFile, StandardCharsets.UTF_8));
      Files.delete(tempFile);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to sync store " + store.id + " to legacy system", e);
    }
  }
}
