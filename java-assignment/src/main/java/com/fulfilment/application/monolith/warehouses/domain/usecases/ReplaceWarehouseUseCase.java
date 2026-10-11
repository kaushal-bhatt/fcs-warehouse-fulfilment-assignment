package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.warehouses.domain.services.WarehouseValidator;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import org.jboss.logging.Logger;

/**
 * Archives the active warehouse with the given business unit code and creates its successor with
 * the same code, keeping the archived one as history. Must run in a single transaction.
 */
@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private static final Logger LOGGER = Logger.getLogger(ReplaceWarehouseUseCase.class);

  private final WarehouseStore warehouseStore;
  private final WarehouseValidator validator;
  private final ArchiveWarehouseOperation archiveWarehouseOperation;

  public ReplaceWarehouseUseCase(
      WarehouseStore warehouseStore,
      WarehouseValidator validator,
      ArchiveWarehouseOperation archiveWarehouseOperation) {
    this.warehouseStore = warehouseStore;
    this.validator = validator;
    this.archiveWarehouseOperation = archiveWarehouseOperation;
  }

  @Override
  public void replace(Warehouse newWarehouse) {
    validator.validateFields(newWarehouse);

    Warehouse current = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (current == null) {
      throw new WarehouseNotFoundException(
          "No active warehouse with business unit code '%s'", newWarehouse.businessUnitCode);
    }

    if (newWarehouse.capacity < current.stock) {
      throw new WarehouseValidationException(
          "Capacity %d cannot accommodate the stock %d of the replaced warehouse",
          newWarehouse.capacity,
          current.stock);
    }
    if (!newWarehouse.stock.equals(current.stock)) {
      throw new WarehouseValidationException(
          "Stock %d must match the stock %d of the replaced warehouse",
          newWarehouse.stock,
          current.stock);
    }

    validator.validateLocation(newWarehouse, current);

    archiveWarehouseOperation.archive(current);

    newWarehouse.createdAt = LocalDateTime.now();
    newWarehouse.archivedAt = null;
    warehouseStore.create(newWarehouse);

    LOGGER.infof(
        "Warehouse %s replaced: %s (capacity %d) -> %s (capacity %d)",
        newWarehouse.businessUnitCode,
        current.location,
        current.capacity,
        newWarehouse.location,
        newWarehouse.capacity);
  }
}
