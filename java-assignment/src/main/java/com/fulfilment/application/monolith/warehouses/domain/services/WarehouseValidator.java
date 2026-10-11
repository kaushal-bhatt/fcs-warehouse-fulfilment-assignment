package com.fulfilment.application.monolith.warehouses.domain.services;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

/** Business rules shared by the create and replace use cases. */
@ApplicationScoped
public class WarehouseValidator {

  private final LocationResolver locationResolver;
  private final WarehouseStore warehouseStore;

  public WarehouseValidator(LocationResolver locationResolver, WarehouseStore warehouseStore) {
    this.locationResolver = locationResolver;
    this.warehouseStore = warehouseStore;
  }

  /** Rules that only depend on the warehouse itself. */
  public void validateFields(Warehouse warehouse) {
    require(!isBlank(warehouse.businessUnitCode), "Business unit code is required");
    require(!isBlank(warehouse.location), "Location is required");
    require(
        warehouse.capacity != null && warehouse.capacity > 0, "Capacity must be greater than 0");
    require(warehouse.stock != null && warehouse.stock >= 0, "Stock must be 0 or greater");
    require(
        warehouse.stock <= warehouse.capacity,
        "Stock (%d) exceeds the warehouse capacity (%d)",
        warehouse.stock,
        warehouse.capacity);
  }

  /**
   * Checks that the location exists, still accepts another warehouse and has enough capacity left.
   *
   * @param replaced the active warehouse being archived in the same operation, excluded from the
   *     location limits; {@code null} when creating
   */
  public void validateLocation(Warehouse candidate, Warehouse replaced) {
    Location location = locationResolver.resolveByIdentifier(candidate.location);
    require(location != null, "Location '%s' does not exist", candidate.location);

    List<Warehouse> others =
        warehouseStore.findActiveByLocation(location.identification).stream()
            .filter(w -> replaced == null || !w.businessUnitCode.equals(replaced.businessUnitCode))
            .toList();

    require(
        others.size() < location.maxNumberOfWarehouses,
        "Location '%s' already has the maximum number of warehouses (%d)",
        location.identification,
        location.maxNumberOfWarehouses);

    int usedCapacity = others.stream().mapToInt(w -> w.capacity).sum();
    int availableCapacity = location.maxCapacity - usedCapacity;
    require(
        candidate.capacity <= availableCapacity,
        "Capacity %d exceeds the available capacity %d of location '%s'",
        candidate.capacity,
        Math.max(availableCapacity, 0),
        location.identification);
  }

  private static void require(boolean condition, String format, Object... args) {
    if (!condition) {
      throw new WarehouseValidationException(format, args);
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
