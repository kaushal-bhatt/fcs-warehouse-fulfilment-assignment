package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.warehouses.domain.services.WarehouseValidator;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private static final Logger LOGGER = Logger.getLogger(CreateWarehouseUseCase.class);

  private final WarehouseStore warehouseStore;
  private final WarehouseValidator validator;

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, WarehouseValidator validator) {
    this.warehouseStore = warehouseStore;
    this.validator = validator;
  }

  @Override
  public void create(Warehouse warehouse) {
    validator.validateFields(warehouse);

    // a code is only reused through replacement, so archived codes count as taken
    if (warehouseStore.existsByBusinessUnitCode(warehouse.businessUnitCode)) {
      throw new WarehouseValidationException(
          "Business unit code '%s' already exists", warehouse.businessUnitCode);
    }

    validator.validateLocation(warehouse, null);

    warehouse.createdAt = LocalDateTime.now();
    warehouse.archivedAt = null;
    warehouseStore.create(warehouse);

    LOGGER.infof(
        "Warehouse %s created at %s (capacity %d, stock %d)",
        warehouse.businessUnitCode, warehouse.location, warehouse.capacity, warehouse.stock);
  }
}
