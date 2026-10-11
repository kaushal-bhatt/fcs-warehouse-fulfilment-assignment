package com.fulfilment.application.monolith;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class HealthCheckTest {

  @Test
  public void livenessShouldBeUp() {
    given().when().get("/q/health/live").then().statusCode(200).body("status", is("UP"));
  }

  @Test
  public void readinessShouldIncludeDatabase() {
    given()
        .when()
        .get("/q/health/ready")
        .then()
        .statusCode(200)
        .body("status", is("UP"))
        .body("checks.name", hasItem("Database connections health check"));
  }
}
