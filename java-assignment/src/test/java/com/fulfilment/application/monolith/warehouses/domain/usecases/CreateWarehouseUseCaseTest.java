package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.warehouses.domain.services.WarehouseValidator;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class CreateWarehouseUseCaseTest {

  private static final String LOCATION = "AMSTERDAM-001";

  private WarehouseStore warehouseStore;
  private LocationResolver locationResolver;
  private CreateWarehouseUseCase useCase;

  @BeforeEach
  public void setUp() {
    warehouseStore = mock(WarehouseStore.class);
    locationResolver = mock(LocationResolver.class);
    useCase =
        new CreateWarehouseUseCase(
            warehouseStore, new WarehouseValidator(locationResolver, warehouseStore));

    // AMSTERDAM-001: up to 2 warehouses, 100 capacity in total
    when(locationResolver.resolveByIdentifier(LOCATION)).thenReturn(new Location(LOCATION, 2, 100));
    when(warehouseStore.findActiveByLocation(LOCATION)).thenReturn(List.of());
  }

  @Test
  public void shouldCreateValidWarehouse() {
    Warehouse warehouse = warehouse("MWH.100", LOCATION, 50, 10);

    useCase.create(warehouse);

    verify(warehouseStore).create(warehouse);
    assertNotNull(warehouse.createdAt);
    assertNull(warehouse.archivedAt);
  }

  @Test
  public void shouldAllowStockEqualToCapacityAndCapacityEqualToRemaining() {
    when(warehouseStore.findActiveByLocation(LOCATION))
        .thenReturn(List.of(warehouse("MWH.001", LOCATION, 60, 0)));

    useCase.create(warehouse("MWH.100", LOCATION, 40, 40));

    verify(warehouseStore).create(any());
  }

  @Test
  public void shouldRejectExistingBusinessUnitCode() {
    when(warehouseStore.existsByBusinessUnitCode("MWH.001")).thenReturn(true);

    assertRejected(warehouse("MWH.001", LOCATION, 10, 1), "Business unit code 'MWH.001' already exists");
  }

  @Test
  public void shouldRejectUnknownLocation() {
    assertRejected(warehouse("MWH.100", "PARIS-001", 10, 1), "Location 'PARIS-001' does not exist");
  }

  @Test
  public void shouldRejectWhenLocationHasMaxNumberOfWarehouses() {
    when(warehouseStore.findActiveByLocation(LOCATION))
        .thenReturn(
            List.of(warehouse("MWH.001", LOCATION, 10, 1), warehouse("MWH.002", LOCATION, 10, 1)));

    assertRejected(
        warehouse("MWH.100", LOCATION, 10, 1),
        "Location 'AMSTERDAM-001' already has the maximum number of warehouses (2)");
  }

  @Test
  public void shouldRejectCapacityAboveLocationRemainingCapacity() {
    when(warehouseStore.findActiveByLocation(LOCATION))
        .thenReturn(List.of(warehouse("MWH.001", LOCATION, 70, 1)));

    assertRejected(
        warehouse("MWH.100", LOCATION, 31, 1),
        "Capacity 31 exceeds the available capacity 30 of location 'AMSTERDAM-001'");
  }

  @ParameterizedTest(name = "{1}")
  @MethodSource("invalidFields")
  public void shouldRejectInvalidFields(Warehouse warehouse, String expectedMessage) {
    assertRejected(warehouse, expectedMessage);
  }

  static Stream<Arguments> invalidFields() {
    return Stream.of(
        Arguments.of(warehouse(null, LOCATION, 10, 1), "Business unit code is required"),
        Arguments.of(warehouse(" ", LOCATION, 10, 1), "Business unit code is required"),
        Arguments.of(warehouse("MWH.100", null, 10, 1), "Location is required"),
        Arguments.of(warehouse("MWH.100", LOCATION, null, 1), "Capacity must be greater than 0"),
        Arguments.of(warehouse("MWH.100", LOCATION, 0, 0), "Capacity must be greater than 0"),
        Arguments.of(warehouse("MWH.100", LOCATION, 10, null), "Stock must be 0 or greater"),
        Arguments.of(warehouse("MWH.100", LOCATION, 10, -1), "Stock must be 0 or greater"),
        Arguments.of(
            warehouse("MWH.100", LOCATION, 10, 11),
            "Stock (11) exceeds the warehouse capacity (10)"));
  }

  private void assertRejected(Warehouse warehouse, String expectedMessage) {
    var exception =
        assertThrows(WarehouseValidationException.class, () -> useCase.create(warehouse));
    assertEquals(expectedMessage, exception.getMessage());
    verify(warehouseStore, never()).create(any());
  }

  static Warehouse warehouse(String code, String location, Integer capacity, Integer stock) {
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = code;
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = stock;
    return warehouse;
  }
}
