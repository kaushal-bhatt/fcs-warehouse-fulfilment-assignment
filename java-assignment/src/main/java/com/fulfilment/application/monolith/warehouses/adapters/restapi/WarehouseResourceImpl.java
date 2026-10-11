package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.warehouse.api.WarehouseResource;
import com.warehouse.api.beans.Warehouse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.jboss.resteasy.reactive.ResponseStatus;

/**
 * REST adapter for the generated Warehouse API. Commands go through the domain use cases; simple
 * reads go straight to the store port.
 */
@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {

  @Inject WarehouseStore warehouseStore;

  @Inject CreateWarehouseOperation createWarehouseOperation;

  @Inject ArchiveWarehouseOperation archiveWarehouseOperation;

  @Inject ReplaceWarehouseOperation replaceWarehouseOperation;

  @Override
  public List<Warehouse> listAllWarehousesUnits() {
    return warehouseStore.findAllActive().stream().map(WarehouseApiMapper::toResponse).toList();
  }

  @Override
  @Transactional
  @ResponseStatus(201)
  public Warehouse createANewWarehouseUnit(@NotNull Warehouse data) {
    var warehouse = WarehouseApiMapper.toDomain(data);
    createWarehouseOperation.create(warehouse);
    return WarehouseApiMapper.toResponse(warehouse);
  }

  @Override
  public Warehouse getAWarehouseUnitByID(String id) {
    return WarehouseApiMapper.toResponse(findActive(id));
  }

  @Override
  @Transactional
  public void archiveAWarehouseUnitByID(String id) {
    archiveWarehouseOperation.archive(findActive(id));
  }

  @Override
  @Transactional
  public Warehouse replaceTheCurrentActiveWarehouse(
      String businessUnitCode, @NotNull Warehouse data) {
    var warehouse = WarehouseApiMapper.toDomain(data);
    if (warehouse.businessUnitCode == null) {
      warehouse.businessUnitCode = businessUnitCode;
    } else if (!warehouse.businessUnitCode.equals(businessUnitCode)) {
      throw new WarehouseValidationException(
          "Business unit code '%s' in body does not match '%s' in path",
          warehouse.businessUnitCode,
          businessUnitCode);
    }
    replaceWarehouseOperation.replace(warehouse);
    return WarehouseApiMapper.toResponse(warehouse);
  }

  private com.fulfilment.application.monolith.warehouses.domain.models.Warehouse findActive(
      String id) {
    var warehouse = warehouseStore.findActiveById(parseId(id));
    if (warehouse == null) {
      throw notFound(id);
    }
    return warehouse;
  }

  private static Long parseId(String id) {
    try {
      return Long.valueOf(id);
    } catch (NumberFormatException e) {
      throw notFound(id);
    }
  }

  private static WarehouseNotFoundException notFound(String id) {
    return new WarehouseNotFoundException("Warehouse with id '%s' does not exist", id);
  }
}
