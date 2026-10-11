package com.fulfilment.application.monolith.products;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@QuarkusTest
public class ProductEndpointTest {

  private static final String PATH = "product";

  @Test
  public void testCrudProduct() {
    final String path = "product";

    // List all, should have all 3 products the database has initially:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"), containsString("KALLAX"), containsString("BESTÅ"));

    // Delete the TONSTAD:
    given().when().delete(path + "/1").then().statusCode(204);

    // List all, TONSTAD should be missing now:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(not(containsString("TONSTAD")), containsString("KALLAX"), containsString("BESTÅ"));
  }

  @Test
  public void createGetAndUpdateShouldWork() {
    int id =
        given()
            .contentType(ContentType.JSON)
            .body("{\"name\":\"PRODUCT-NEW\",\"description\":\"desk\",\"price\":49.95,\"stock\":3}")
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    given().when().get(PATH + "/" + id).then().statusCode(200).body("name", is("PRODUCT-NEW"));

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"PRODUCT-RENAMED\",\"price\":39.95,\"stock\":7}")
        .when()
        .put(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("name", is("PRODUCT-RENAMED"))
        .body("stock", is(7));
  }

  @Test
  public void duplicateNameShouldReturnConflict() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"KALLAX\",\"stock\":1}")
        .when()
        .post(PATH)
        .then()
        .statusCode(409)
        .body("error", is("A record with the same unique value already exists"));
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource(
      delimiter = '|',
      value = {
        "id set | {\"id\":5,\"name\":\"PRODUCT-ID\"} | Id was invalidly set on request.",
        "missing name | {\"stock\":1} | Product Name was not set on request.",
        "name too long | {\"name\":\"ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNO\"} | Product Name must be between 1 and 40 characters.",
        "negative stock | {\"name\":\"PRODUCT-NEG\",\"stock\":-1} | Product stock cannot be negative.",
        "negative price | {\"name\":\"PRODUCT-NEG\",\"price\":-0.01} | Product price cannot be negative."
      })
  public void createWithInvalidValuesShouldBeRejected(String label, String body, String error) {
    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(422)
        .body("error", is(error));
  }

  @Test
  public void unknownProductShouldReturnNotFound() {
    given().when().get(PATH + "/99999").then().statusCode(404);
    given().when().delete(PATH + "/99999").then().statusCode(404);
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"GHOST\"}")
        .when()
        .put(PATH + "/99999")
        .then()
        .statusCode(404)
        .body("error", is("Product with id of 99999 does not exist."));
  }

  @Test
  public void updateWithoutNameShouldBeRejected() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"stock\":2}")
        .when()
        .put(PATH + "/2")
        .then()
        .statusCode(422);
  }
}
