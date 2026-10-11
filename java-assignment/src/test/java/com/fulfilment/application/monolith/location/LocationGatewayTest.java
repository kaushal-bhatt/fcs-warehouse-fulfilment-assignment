package com.fulfilment.application.monolith.location;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

public class LocationGatewayTest {

  private final LocationGateway locationGateway = new LocationGateway();

  @Test
  public void testWhenResolveExistingLocationShouldReturn() {
    Location location = locationGateway.resolveByIdentifier("ZWOLLE-001");

    assertNotNull(location);
    assertEquals("ZWOLLE-001", location.identification);
    assertEquals(1, location.maxNumberOfWarehouses);
    assertEquals(40, location.maxCapacity);
  }

  @Test
  public void testWhenResolveAnotherExistingLocationShouldReturnItsOwnLimits() {
    Location location = locationGateway.resolveByIdentifier("AMSTERDAM-001");

    assertNotNull(location);
    assertEquals(5, location.maxNumberOfWarehouses);
    assertEquals(100, location.maxCapacity);
  }

  @ParameterizedTest
  @ValueSource(strings = {"UNKNOWN-001", "zwolle-001", " ZWOLLE-001"})
  public void testWhenResolveUnknownLocationShouldReturnNull(String identifier) {
    assertNull(locationGateway.resolveByIdentifier(identifier));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  public void testWhenResolveBlankIdentifierShouldReturnNull(String identifier) {
    assertNull(locationGateway.resolveByIdentifier(identifier));
  }
}
