package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@QuarkusTest
public class WarehouseResourceTest {

  private static final String PATH = "warehouse";

  @Test
  public void listShouldReturnActiveWarehouses() {
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(containsString("MWH.012"), containsString("MWH.023"));
  }

  @Test
  public void getByIdShouldReturnWarehouse() {
    given()
        .when()
        .get(PATH + "/2")
        .then()
        .statusCode(200)
        .body("id", is("2"))
        .body("businessUnitCode", is("MWH.012"))
        .body("location", is("AMSTERDAM-001"));
  }

  @ParameterizedTest
  @CsvSource({"999999", "not-a-number"})
  public void getUnknownIdShouldReturnNotFound(String id) {
    given()
        .when()
        .get(PATH + "/" + id)
        .then()
        .statusCode(404)
        .body("error", containsString("does not exist"));
  }

  @Test
  public void createShouldReturnCreatedWarehouse() {
    String id =
        given()
            .contentType(ContentType.JSON)
            .body(body("RES.001", "VETSBY-001", 50, 20))
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("businessUnitCode", is("RES.001"))
            .extract()
            .path("id");

    given().when().get(PATH + "/" + id).then().statusCode(200).body("location", is("VETSBY-001"));
  }

  @ParameterizedTest(name = "{4}")
  @CsvSource({
    "MWH.012, EINDHOVEN-001, 10, 1, Business unit code 'MWH.012' already exists",
    "RES.002, PARIS-001, 10, 1, Location 'PARIS-001' does not exist",
    "RES.003, TILBURG-001, 10, 1, Location 'TILBURG-001' already has the maximum number of warehouses (1)",
    "RES.004, AMSTERDAM-002, 80, 1, Capacity 80 exceeds the available capacity 75 of location 'AMSTERDAM-002'",
    "RES.005, AMSTERDAM-002, 10, 11, Stock (11) exceeds the warehouse capacity (10)"
  })
  public void createBreakingBusinessRuleShouldReturnBadRequest(
      String code, String location, int capacity, int stock, String expectedError) {
    given()
        .contentType(ContentType.JSON)
        .body(body(code, location, capacity, stock))
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", is(expectedError));
  }

  @Test
  public void createWithIdShouldReturnBadRequest() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"id\":\"7\",\"businessUnitCode\":\"RES.006\",\"location\":\"AMSTERDAM-002\",\"capacity\":10,\"stock\":1}")
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", is("Id must not be set on request"));
  }

  @Test
  public void createWithoutBodyShouldReturnBadRequest() {
    given()
        .contentType(ContentType.JSON)
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", is("Request body is required"));
  }

  @Test
  public void archiveShouldHideWarehouseAndFreeItsLocationSlot() {
    // HELMOND-001 accepts a single warehouse
    String id = create("RES.010", "HELMOND-001", 20, 5);
    given()
        .contentType(ContentType.JSON)
        .body(body("RES.011", "HELMOND-001", 20, 5))
        .when()
        .post(PATH)
        .then()
        .statusCode(400);

    given().when().delete(PATH + "/" + id).then().statusCode(204);

    given().when().get(PATH + "/" + id).then().statusCode(404);
    given().when().get(PATH).then().statusCode(200).body(not(containsString("RES.010")));
    create("RES.011", "HELMOND-001", 20, 5);
  }

  @Test
  public void archiveTwiceShouldReturnNotFound() {
    String id = create("RES.012", "EINDHOVEN-001", 10, 1);

    given().when().delete(PATH + "/" + id).then().statusCode(204);
    given().when().delete(PATH + "/" + id).then().statusCode(404);
  }

  @ParameterizedTest
  @CsvSource({"999999", "not-a-number"})
  public void archiveUnknownIdShouldReturnNotFound(String id) {
    given().when().delete(PATH + "/" + id).then().statusCode(404);
  }

  @Test
  public void archivedBusinessUnitCodeCannotBeReusedByCreate() {
    String id = create("RES.013", "EINDHOVEN-001", 10, 1);
    given().when().delete(PATH + "/" + id).then().statusCode(204);

    given()
        .contentType(ContentType.JSON)
        .body(body("RES.013", "EINDHOVEN-001", 10, 1))
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", is("Business unit code 'RES.013' already exists"));
  }

  private static String create(String code, String location, int capacity, int stock) {
    return given()
        .contentType(ContentType.JSON)
        .body(body(code, location, capacity, stock))
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .extract()
        .path("id");
  }

  private static String body(String code, String location, int capacity, int stock) {
    return String.format(
        "{\"businessUnitCode\":\"%s\",\"location\":\"%s\",\"capacity\":%d,\"stock\":%d}",
        code, location, capacity, stock);
  }
}
