package com.fulfilment.application.monolith.warehouses.adapters.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** Runs against the Dev Services PostgreSQL; every test is rolled back. */
@QuarkusTest
@TestTransaction
public class WarehouseRepositoryTest {

  @Inject WarehouseRepository repository;

  @Test
  public void createShouldPersistAndAssignId() {
    Warehouse warehouse = warehouse("REPO.001", "HELMOND-001");

    repository.create(warehouse);

    assertNotNull(warehouse.id);
    Warehouse stored = repository.findActiveById(warehouse.id);
    assertEquals("REPO.001", stored.businessUnitCode);
    assertEquals("HELMOND-001", stored.location);
    assertEquals(20, stored.capacity);
  }

  @Test
  public void archivedWarehousesShouldOnlyBeVisibleToExistsCheck() {
    Warehouse warehouse = warehouse("REPO.002", "HELMOND-001");
    repository.create(warehouse);

    warehouse.archivedAt = LocalDateTime.now();
    repository.update(warehouse);

    assertNull(repository.findActiveById(warehouse.id));
    assertNull(repository.findByBusinessUnitCode("REPO.002"));
    assertTrue(repository.findActiveByLocation("HELMOND-001").isEmpty());
    assertFalse(
        repository.findAllActive().stream().anyMatch(w -> w.businessUnitCode.equals("REPO.002")));
    assertTrue(repository.getAll().stream().anyMatch(w -> w.businessUnitCode.equals("REPO.002")));
    assertTrue(repository.existsByBusinessUnitCode("REPO.002"));
  }

  @Test
  public void findByBusinessUnitCodeShouldReturnActiveSeedWarehouse() {
    Warehouse warehouse = repository.findByBusinessUnitCode("MWH.012");

    assertEquals("AMSTERDAM-001", warehouse.location);
    assertFalse(repository.existsByBusinessUnitCode("UNKNOWN"));
  }

  @Test
  public void updateUnknownIdShouldFail() {
    Warehouse warehouse = warehouse("REPO.003", "HELMOND-001");
    warehouse.id = 999_999L;

    assertThrows(WarehouseNotFoundException.class, () -> repository.update(warehouse));
  }

  @Test
  public void removeShouldDelete() {
    Warehouse warehouse = warehouse("REPO.004", "HELMOND-001");
    repository.create(warehouse);

    repository.remove(warehouse);

    assertFalse(repository.existsByBusinessUnitCode("REPO.004"));
  }

  private static Warehouse warehouse(String code, String location) {
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = code;
    warehouse.location = location;
    warehouse.capacity = 20;
    warehouse.stock = 5;
    warehouse.createdAt = LocalDateTime.now();
    return warehouse;
  }
}
