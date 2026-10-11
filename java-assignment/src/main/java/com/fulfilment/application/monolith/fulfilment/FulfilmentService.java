package com.fulfilment.application.monolith.fulfilment;

import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

/**
 * Assigns warehouses as fulfilment units of products for stores. Only assignments to active
 * warehouses count towards the limits; an archived warehouse frees its slots.
 */
@ApplicationScoped
public class FulfilmentService {

  static final int MAX_WAREHOUSES_PER_PRODUCT_AND_STORE = 2;
  static final int MAX_WAREHOUSES_PER_STORE = 3;
  static final int MAX_PRODUCTS_PER_WAREHOUSE = 5;

  private static final Logger LOGGER = Logger.getLogger(FulfilmentService.class);

  private final FulfilmentAssignmentRepository assignmentRepository;
  private final ProductRepository productRepository;
  private final WarehouseStore warehouseStore;

  public FulfilmentService(
      FulfilmentAssignmentRepository assignmentRepository,
      ProductRepository productRepository,
      WarehouseStore warehouseStore) {
    this.assignmentRepository = assignmentRepository;
    this.productRepository = productRepository;
    this.warehouseStore = warehouseStore;
  }

  public List<FulfilmentAssignment> listActive(Long storeId) {
    List<FulfilmentAssignment> assignments =
        storeId == null ? assignmentRepository.listAll() : assignmentRepository.findByStore(storeId);
    Set<String> activeCodes = activeBusinessUnitCodes();
    return assignments.stream().filter(a -> activeCodes.contains(a.businessUnitCode)).toList();
  }

  @Transactional
  public FulfilmentAssignment assign(FulfilmentAssignment request) {
    validateRequest(request);

    Set<String> activeCodes = activeBusinessUnitCodes();
    List<FulfilmentAssignment> storeAssignments =
        assignmentRepository.findByStore(request.storeId).stream()
            .filter(a -> activeCodes.contains(a.businessUnitCode))
            .toList();

    boolean alreadyAssigned =
        storeAssignments.stream()
            .anyMatch(
                a ->
                    a.productId.equals(request.productId)
                        && a.businessUnitCode.equals(request.businessUnitCode));
    if (alreadyAssigned) {
      throw conflict(
          "Warehouse %s already fulfils product %d for store %d",
          request.businessUnitCode, request.productId, request.storeId);
    }

    long warehousesForProduct =
        storeAssignments.stream()
            .filter(a -> a.productId.equals(request.productId))
            .map(a -> a.businessUnitCode)
            .distinct()
            .count();
    if (warehousesForProduct >= MAX_WAREHOUSES_PER_PRODUCT_AND_STORE) {
      throw conflict(
          "Product %d is already fulfilled by %d warehouses for store %d",
          request.productId, MAX_WAREHOUSES_PER_PRODUCT_AND_STORE, request.storeId);
    }

    Set<String> storeWarehouses =
        storeAssignments.stream().map(a -> a.businessUnitCode).collect(Collectors.toSet());
    if (!storeWarehouses.contains(request.businessUnitCode)
        && storeWarehouses.size() >= MAX_WAREHOUSES_PER_STORE) {
      throw conflict(
          "Store %d is already fulfilled by %d warehouses",
          request.storeId, MAX_WAREHOUSES_PER_STORE);
    }

    Set<Long> warehouseProducts =
        assignmentRepository.findByWarehouse(request.businessUnitCode).stream()
            .map(a -> a.productId)
            .collect(Collectors.toSet());
    if (!warehouseProducts.contains(request.productId)
        && warehouseProducts.size() >= MAX_PRODUCTS_PER_WAREHOUSE) {
      throw conflict(
          "Warehouse %s already stores %d product types",
          request.businessUnitCode, MAX_PRODUCTS_PER_WAREHOUSE);
    }

    request.createdAt = LocalDateTime.now();
    assignmentRepository.persist(request);
    LOGGER.infof(
        "Warehouse %s now fulfils product %d for store %d",
        request.businessUnitCode, request.productId, request.storeId);
    return request;
  }

  @Transactional
  public void unassign(Long id) {
    if (!assignmentRepository.deleteById(id)) {
      throw new WebApplicationException(
          "Fulfilment assignment with id of " + id + " does not exist.", 404);
    }
  }

  private void validateRequest(FulfilmentAssignment request) {
    if (request == null) {
      throw new WebApplicationException("Request body is required.", 400);
    }
    if (request.id != null) {
      throw new WebApplicationException("Id was invalidly set on request.", 400);
    }
    if (request.storeId == null || request.productId == null || request.businessUnitCode == null) {
      throw new WebApplicationException(
          "storeId, productId and businessUnitCode are required.", 400);
    }
    if (Store.findById(request.storeId) == null) {
      throw new WebApplicationException(
          "Store with id of " + request.storeId + " does not exist.", 404);
    }
    if (productRepository.findById(request.productId) == null) {
      throw new WebApplicationException(
          "Product with id of " + request.productId + " does not exist.", 404);
    }
    if (warehouseStore.findByBusinessUnitCode(request.businessUnitCode) == null) {
      throw new WebApplicationException(
          "No active warehouse with business unit code '" + request.businessUnitCode + "'.", 404);
    }
  }

  private Set<String> activeBusinessUnitCodes() {
    return warehouseStore.findAllActive().stream()
        .map(warehouse -> warehouse.businessUnitCode)
        .collect(Collectors.toSet());
  }

  private static WebApplicationException conflict(String format, Object... args) {
    return new WebApplicationException(String.format(format, args), 409);
  }
}
