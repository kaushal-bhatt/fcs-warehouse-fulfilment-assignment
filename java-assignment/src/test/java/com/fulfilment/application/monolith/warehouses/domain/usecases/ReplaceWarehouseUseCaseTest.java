package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static com.fulfilment.application.monolith.warehouses.domain.usecases.CreateWarehouseUseCaseTest.warehouse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseDomainException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.warehouses.domain.services.WarehouseValidator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReplaceWarehouseUseCaseTest {

  private static final String TILBURG = "TILBURG-001";
  private static final String AMSTERDAM = "AMSTERDAM-002";

  private WarehouseStore warehouseStore;
  private ArchiveWarehouseOperation archiveOperation;
  private ReplaceWarehouseUseCase useCase;
  private Warehouse current;

  @BeforeEach
  public void setUp() {
    warehouseStore = mock(WarehouseStore.class);
    archiveOperation = mock(ArchiveWarehouseOperation.class);
    LocationResolver locationResolver = mock(LocationResolver.class);
    useCase =
        new ReplaceWarehouseUseCase(
            warehouseStore,
            new WarehouseValidator(locationResolver, warehouseStore),
            archiveOperation);

    // TILBURG-001 accepts a single warehouse with 40 capacity, already used by the current one
    when(locationResolver.resolveByIdentifier(TILBURG)).thenReturn(new Location(TILBURG, 1, 40));
    when(locationResolver.resolveByIdentifier(AMSTERDAM))
        .thenReturn(new Location(AMSTERDAM, 3, 75));

    current = warehouse("MWH.023", TILBURG, 30, 27);
    current.id = 3L;
    when(warehouseStore.findByBusinessUnitCode("MWH.023")).thenReturn(current);
    when(warehouseStore.findActiveByLocation(TILBURG)).thenReturn(List.of(current));
    when(warehouseStore.findActiveByLocation(AMSTERDAM)).thenReturn(List.of());
  }

  @Test
  public void shouldArchiveCurrentThenCreateReplacementInSameLocation() {
    // same location: the warehouse being replaced does not count against the location limits
    Warehouse replacement = warehouse("MWH.023", TILBURG, 40, 27);

    useCase.replace(replacement);

    var order = inOrder(archiveOperation, warehouseStore);
    order.verify(archiveOperation).archive(current);
    order.verify(warehouseStore).create(replacement);
    assertNotNull(replacement.createdAt);
  }

  @Test
  public void shouldReplaceIntoAnotherLocation() {
    Warehouse replacement = warehouse("MWH.023", AMSTERDAM, 60, 27);

    useCase.replace(replacement);

    verify(archiveOperation).archive(current);
    verify(warehouseStore).create(replacement);
  }

  @Test
  public void shouldFailWhenNoActiveWarehouseHasTheCode() {
    assertRejected(
        warehouse("MWH.999", AMSTERDAM, 40, 5),
        WarehouseNotFoundException.class,
        "No active warehouse with business unit code 'MWH.999'");
  }

  @Test
  public void shouldRejectCapacityThatCannotAccommodateCurrentStock() {
    assertRejected(
        warehouse("MWH.023", AMSTERDAM, 20, 5),
        WarehouseValidationException.class,
        "Capacity 20 cannot accommodate the stock 27 of the replaced warehouse");
  }

  @Test
  public void shouldRejectStockThatDoesNotMatchCurrentStock() {
    assertRejected(
        warehouse("MWH.023", TILBURG, 40, 20),
        WarehouseValidationException.class,
        "Stock 20 must match the stock 27 of the replaced warehouse");
  }

  @Test
  public void shouldApplyLocationRulesToReplacement() {
    assertRejected(
        warehouse("MWH.023", TILBURG, 41, 27),
        WarehouseValidationException.class,
        "Capacity 41 exceeds the available capacity 40 of location 'TILBURG-001'");
  }

  @Test
  public void shouldRejectFullTargetLocation() {
    when(warehouseStore.findActiveByLocation(AMSTERDAM))
        .thenReturn(
            List.of(
                warehouse("A", AMSTERDAM, 5, 0),
                warehouse("B", AMSTERDAM, 5, 0),
                warehouse("C", AMSTERDAM, 5, 0)));

    assertRejected(
        warehouse("MWH.023", AMSTERDAM, 40, 27),
        WarehouseValidationException.class,
        "Location 'AMSTERDAM-002' already has the maximum number of warehouses (3)");
  }

  @Test
  public void shouldValidateFieldsBeforeLookingUpCurrent() {
    assertRejected(
        warehouse("MWH.023", TILBURG, null, 27),
        WarehouseValidationException.class,
        "Capacity must be greater than 0");
    verify(warehouseStore, never()).findByBusinessUnitCode(any());
  }

  private void assertRejected(
      Warehouse replacement, Class<? extends WarehouseDomainException> type, String message) {
    var exception = assertThrows(type, () -> useCase.replace(replacement));
    assertEquals(message, exception.getMessage());
    verify(archiveOperation, never()).archive(any());
    verify(warehouseStore, never()).create(any());
  }
}
