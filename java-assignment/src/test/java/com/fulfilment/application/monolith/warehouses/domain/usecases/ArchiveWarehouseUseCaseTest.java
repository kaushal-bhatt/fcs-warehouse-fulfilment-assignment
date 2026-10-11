package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ArchiveWarehouseUseCaseTest {

  private WarehouseStore warehouseStore;
  private ArchiveWarehouseUseCase useCase;

  @BeforeEach
  public void setUp() {
    warehouseStore = mock(WarehouseStore.class);
    useCase = new ArchiveWarehouseUseCase(warehouseStore);
  }

  @Test
  public void shouldSetArchivedAtAndUpdate() {
    Warehouse warehouse = activeWarehouse();
    LocalDateTime before = LocalDateTime.now();

    useCase.archive(warehouse);

    assertNotNull(warehouse.archivedAt);
    assertFalse(warehouse.archivedAt.isBefore(before));
    verify(warehouseStore).update(warehouse);
  }

  @Test
  public void shouldRejectAlreadyArchivedWarehouse() {
    Warehouse warehouse = activeWarehouse();
    LocalDateTime archivedAt = LocalDateTime.now().minusDays(1);
    warehouse.archivedAt = archivedAt;

    var exception =
        assertThrows(WarehouseValidationException.class, () -> useCase.archive(warehouse));

    assertEquals("Warehouse MWH.001 is already archived", exception.getMessage());
    assertEquals(archivedAt, warehouse.archivedAt);
    verify(warehouseStore, never()).update(any());
  }

  private static Warehouse activeWarehouse() {
    var warehouse = new Warehouse();
    warehouse.id = 1L;
    warehouse.businessUnitCode = "MWH.001";
    warehouse.location = "ZWOLLE-001";
    warehouse.capacity = 40;
    warehouse.stock = 10;
    return warehouse;
  }
}
