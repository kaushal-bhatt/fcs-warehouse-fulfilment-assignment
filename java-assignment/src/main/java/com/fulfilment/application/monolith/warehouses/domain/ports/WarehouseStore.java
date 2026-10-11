package com.fulfilment.application.monolith.warehouses.domain.ports;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import java.util.List;

public interface WarehouseStore {

  List<Warehouse> getAll();

  /** Warehouses that are not archived. */
  List<Warehouse> findAllActive();

  /** Persists a new warehouse and sets its generated {@code id}. */
  void create(Warehouse warehouse);

  /** Updates the warehouse identified by {@code warehouse.id}. */
  void update(Warehouse warehouse);

  void remove(Warehouse warehouse);

  /** The active warehouse with this business unit code, or {@code null}. */
  Warehouse findByBusinessUnitCode(String buCode);

  /** The active warehouse with this id, or {@code null}. */
  Warehouse findActiveById(Long id);

  List<Warehouse> findActiveByLocation(String location);

  /** Whether the code was ever used, including by archived warehouses. */
  boolean existsByBusinessUnitCode(String buCode);
}
