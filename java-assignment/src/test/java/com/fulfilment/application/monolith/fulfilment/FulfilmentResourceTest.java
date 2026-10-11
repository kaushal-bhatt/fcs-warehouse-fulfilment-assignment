package com.fulfilment.application.monolith.fulfilment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@QuarkusTest
public class FulfilmentResourceTest {

  private static final String PATH = "fulfilment";

  // business unit code -> warehouse id, created once for the whole class
  private static final Map<String, String> warehouses = new HashMap<>();

  @BeforeEach
  public void createWarehouses() {
    if (!warehouses.isEmpty()) {
      return;
    }
    // AMSTERDAM-001 has room for 4 more warehouses, ZWOLLE-002 for 2
    for (String code : List.of("FUL.W1", "FUL.W2", "FUL.W3", "FUL.W4")) {
      warehouses.put(code, createWarehouse(code, "AMSTERDAM-001"));
    }
    warehouses.put("FUL.W5", createWarehouse("FUL.W5", "ZWOLLE-002"));
    warehouses.put("FUL.W6", createWarehouse("FUL.W6", "ZWOLLE-002"));
  }

  @Test
  public void assignShouldReturnCreatedAndBeListed() {
    int store = createStore("FUL-STORE-1");
    int product = createProduct("FUL-PRODUCT-1");

    given()
        .contentType(ContentType.JSON)
        .body(assignment(store, product, "FUL.W5"))
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body("id", notNullValue())
        .body("businessUnitCode", is("FUL.W5"))
        .body("createdAt", notNullValue());

    given()
        .queryParam("storeId", store)
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body("productId", hasItem(product));
  }

  @Test
  public void duplicateAssignmentShouldReturnConflict() {
    int store = createStore("FUL-STORE-2");
    int product = createProduct("FUL-PRODUCT-2");
    assign(store, product, "FUL.W5", 201);

    given()
        .contentType(ContentType.JSON)
        .body(assignment(store, product, "FUL.W5"))
        .when()
        .post(PATH)
        .then()
        .statusCode(409)
        .body(
            "error",
            is(
                "Warehouse FUL.W5 already fulfils product "
                    + product
                    + " for store "
                    + store));
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({
    "unknown store, 999999, 2, MWH.012, Store with id of 999999 does not exist.",
    "unknown product, 1, 999999, MWH.012, Product with id of 999999 does not exist.",
    "unknown warehouse, 1, 2, NOPE, No active warehouse with business unit code 'NOPE'."
  })
  public void unknownReferencesShouldReturnNotFound(
      String label, long store, long product, String code, String error) {
    given()
        .contentType(ContentType.JSON)
        .body(assignment(store, product, code))
        .when()
        .post(PATH)
        .then()
        .statusCode(404)
        .body("error", is(error));
  }

  @Test
  public void missingFieldShouldReturnBadRequest() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"storeId\":1,\"productId\":2}")
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", is("storeId, productId and businessUnitCode are required."));
  }

  @Test
  public void productCanBeFulfilledByAtMostTwoWarehousesPerStore() {
    int store = createStore("FUL-STORE-3");
    int product = createProduct("FUL-PRODUCT-3");
    assign(store, product, "FUL.W1", 201);
    assign(store, product, "FUL.W2", 201);

    given()
        .contentType(ContentType.JSON)
        .body(assignment(store, product, "FUL.W3"))
        .when()
        .post(PATH)
        .then()
        .statusCode(409)
        .body(
            "error",
            is(
                "Product "
                    + product
                    + " is already fulfilled by 2 warehouses for store "
                    + store));
  }

  @Test
  public void storeCanBeFulfilledByAtMostThreeWarehouses() {
    int store = createStore("FUL-STORE-4");
    assign(store, createProduct("FUL-PRODUCT-4A"), "FUL.W1", 201);
    assign(store, createProduct("FUL-PRODUCT-4B"), "FUL.W2", 201);
    assign(store, createProduct("FUL-PRODUCT-4C"), "FUL.W3", 201);

    given()
        .contentType(ContentType.JSON)
        .body(assignment(store, createProduct("FUL-PRODUCT-4D"), "FUL.W4"))
        .when()
        .post(PATH)
        .then()
        .statusCode(409)
        .body("error", is("Store " + store + " is already fulfilled by 3 warehouses"));

    // a warehouse that already serves the store does not count as a new one
    assign(store, createProduct("FUL-PRODUCT-4E"), "FUL.W1", 201);
  }

  @Test
  public void warehouseCanStoreAtMostFiveProductTypesAndArchivedWarehouseIsReleased() {
    int store = createStore("FUL-STORE-5");
    int firstProduct = createProduct("FUL-PRODUCT-5-1");
    assign(store, firstProduct, "FUL.W6", 201);
    for (int i = 2; i <= 5; i++) {
      assign(store, createProduct("FUL-PRODUCT-5-" + i), "FUL.W6", 201);
    }

    given()
        .contentType(ContentType.JSON)
        .body(assignment(store, createProduct("FUL-PRODUCT-5-6"), "FUL.W6"))
        .when()
        .post(PATH)
        .then()
        .statusCode(409)
        .body("error", is("Warehouse FUL.W6 already stores 5 product types"));

    // an existing product type may be fulfilled for another store
    assign(createStore("FUL-STORE-5B"), firstProduct, "FUL.W6", 201);

    // once archived, the warehouse can no longer be assigned and its assignments are not listed
    given().when().delete("warehouse/" + warehouses.get("FUL.W6")).then().statusCode(204);
    assign(store, firstProduct, "FUL.W6", 404);
    given().queryParam("storeId", store).when().get(PATH).then().body("size()", is(0));
  }

  @Test
  public void replacingWarehouseKeepsItsAssignments() {
    int store = createStore("FUL-STORE-6");
    int product = createProduct("FUL-PRODUCT-6");
    assign(store, product, "FUL.W5", 201);

    given()
        .contentType(ContentType.JSON)
        .body("{\"location\":\"ZWOLLE-002\",\"capacity\":2,\"stock\":0}")
        .when()
        .post("warehouse/FUL.W5/replacement")
        .then()
        .statusCode(200);

    given()
        .queryParam("storeId", store)
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body("businessUnitCode", hasItem("FUL.W5"));
  }

  @Test
  public void unassignShouldRemoveAssignment() {
    int store = createStore("FUL-STORE-7");
    int id = assign(store, createProduct("FUL-PRODUCT-7"), "FUL.W5", 201);

    given().when().delete(PATH + "/" + id).then().statusCode(204);
    given().when().delete(PATH + "/" + id).then().statusCode(404);
    given().queryParam("storeId", store).when().get(PATH).then().body("size()", is(0));
  }

  private static int assign(long store, long product, String code, int expectedStatus) {
    var response =
        given()
            .contentType(ContentType.JSON)
            .body(assignment(store, product, code))
            .when()
            .post(PATH)
            .then()
            .statusCode(expectedStatus)
            .extract();
    return expectedStatus == 201 ? response.path("id") : -1;
  }

  private static String assignment(long store, long product, String code) {
    return String.format(
        "{\"storeId\":%d,\"productId\":%d,\"businessUnitCode\":\"%s\"}", store, product, code);
  }

  private static int createStore(String name) {
    return given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"" + name + "\",\"quantityProductsInStock\":1}")
        .when()
        .post("store")
        .then()
        .statusCode(201)
        .extract()
        .path("id");
  }

  private static int createProduct(String name) {
    return given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"" + name + "\",\"stock\":1}")
        .when()
        .post("product")
        .then()
        .statusCode(201)
        .extract()
        .path("id");
  }

  private static String createWarehouse(String code, String location) {
    return given()
        .contentType(ContentType.JSON)
        .body(
            String.format(
                "{\"businessUnitCode\":\"%s\",\"location\":\"%s\",\"capacity\":1,\"stock\":0}",
                code, location))
        .when()
        .post("warehouse")
        .then()
        .statusCode(201)
        .extract()
        .path("id");
  }
}
