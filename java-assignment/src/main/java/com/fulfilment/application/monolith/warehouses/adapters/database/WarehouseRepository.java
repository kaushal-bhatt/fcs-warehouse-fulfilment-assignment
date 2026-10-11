package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  private static final String ACTIVE = "archivedAt is null";

  @Override
  public List<Warehouse> getAll() {
    return this.listAll().stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
  public List<Warehouse> findAllActive() {
    return list(ACTIVE, Sort.by("id")).stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
  public void create(Warehouse warehouse) {
    DbWarehouse entity = DbWarehouse.from(warehouse);
    persist(entity);
    warehouse.id = entity.id;
  }

  @Override
  public void update(Warehouse warehouse) {
    Objects.requireNonNull(warehouse.id, "Warehouse id is required for update");
    DbWarehouse entity =
        findByIdOptional(warehouse.id)
            .orElseThrow(
                () ->
                    new WarehouseNotFoundException(
                        "Warehouse with id %d does not exist", warehouse.id));
    entity.copyFrom(warehouse);
  }

  @Override
  public void remove(Warehouse warehouse) {
    deleteById(warehouse.id);
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    return find("businessUnitCode = ?1 and " + ACTIVE, buCode)
        .firstResultOptional()
        .map(DbWarehouse::toWarehouse)
        .orElse(null);
  }

  @Override
  public Warehouse findActiveById(Long id) {
    return find("id = ?1 and " + ACTIVE, id)
        .firstResultOptional()
        .map(DbWarehouse::toWarehouse)
        .orElse(null);
  }

  @Override
  public List<Warehouse> findActiveByLocation(String location) {
    return list("location = ?1 and " + ACTIVE, location).stream()
        .map(DbWarehouse::toWarehouse)
        .toList();
  }

  @Override
  public boolean existsByBusinessUnitCode(String buCode) {
    return count("businessUnitCode", buCode) > 0;
  }
}
