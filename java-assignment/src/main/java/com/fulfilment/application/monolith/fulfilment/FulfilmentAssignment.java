package com.fulfilment.application.monolith.fulfilment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * A warehouse fulfilling a product for a store. The warehouse is referenced by its business unit
 * code rather than its id, so assignments carry over when a warehouse is replaced.
 */
@Entity
@Table(
    name = "fulfilment_assignment",
    uniqueConstraints =
        @UniqueConstraint(columnNames = {"storeId", "productId", "businessUnitCode"}))
public class FulfilmentAssignment {

  @Id @GeneratedValue public Long id;

  @Column(nullable = false)
  public Long storeId;

  @Column(nullable = false)
  public Long productId;

  @Column(nullable = false)
  public String businessUnitCode;

  public LocalDateTime createdAt;
}
