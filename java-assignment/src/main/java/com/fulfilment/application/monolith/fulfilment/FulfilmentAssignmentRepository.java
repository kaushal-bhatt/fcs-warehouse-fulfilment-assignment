package com.fulfilment.application.monolith.fulfilment;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class FulfilmentAssignmentRepository implements PanacheRepository<FulfilmentAssignment> {

  public List<FulfilmentAssignment> findByStore(Long storeId) {
    return list("storeId", storeId);
  }

  public List<FulfilmentAssignment> findByWarehouse(String businessUnitCode) {
    return list("businessUnitCode", businessUnitCode);
  }
}
