package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;

/** Translates between the generated API bean and the domain model. */
final class WarehouseApiMapper {

  private WarehouseApiMapper() {}

  static Warehouse toDomain(com.warehouse.api.beans.Warehouse data) {
    if (data == null) {
      throw new WarehouseValidationException("Request body is required");
    }
    if (data.getId() != null) {
      throw new WarehouseValidationException("Id must not be set on request");
    }
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = data.getBusinessUnitCode();
    warehouse.location = data.getLocation();
    warehouse.capacity = data.getCapacity();
    warehouse.stock = data.getStock();
    return warehouse;
  }

  static com.warehouse.api.beans.Warehouse toResponse(Warehouse warehouse) {
    var response = new com.warehouse.api.beans.Warehouse();
    response.setId(warehouse.id == null ? null : String.valueOf(warehouse.id));
    response.setBusinessUnitCode(warehouse.businessUnitCode);
    response.setLocation(warehouse.location);
    response.setCapacity(warehouse.capacity);
    response.setStock(warehouse.stock);
    return response;
  }
}
