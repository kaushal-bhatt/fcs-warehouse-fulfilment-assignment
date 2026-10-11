package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class StoreResourceTest {

  private static final String PATH = "store";

  @InjectMock LegacyStoreManagerGateway legacyGateway;

  @Test
  public void createShouldNotifyLegacySystemWithPersistedStore() {
    int id =
        given()
            .contentType(ContentType.JSON)
            .body("{\"name\":\"STORE-CREATE\",\"quantityProductsInStock\":12}")
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .extract()
            .path("id");

    verify(legacyGateway)
        .createStoreOnLegacySystem(
            argThat(
                s ->
                    s.id == id
                        && "STORE-CREATE".equals(s.name)
                        && s.quantityProductsInStock == 12));
  }

  @Test
  public void createShouldNotNotifyLegacySystemWhenCommitFails() {
    // "TONSTAD" already exists: the unique constraint fails on commit and the transaction rolls back
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"TONSTAD\",\"quantityProductsInStock\":7}")
        .when()
        .post(PATH)
        .then()
        .statusCode(500);

    verify(legacyGateway, never()).createStoreOnLegacySystem(any());
  }

  @Test
  public void createWithIdShouldBeRejected() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"id\":99,\"name\":\"STORE-WITH-ID\"}")
        .when()
        .post(PATH)
        .then()
        .statusCode(422)
        .body("error", containsString("Id was invalidly set"));

    verifyNoInteractions(legacyGateway);
  }

  @Test
  public void updateShouldNotifyLegacySystemWithPersistedEntity() {
    int id = createStore("STORE-UPDATE", 3);

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"STORE-UPDATED\",\"quantityProductsInStock\":8}")
        .when()
        .put(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("name", is("STORE-UPDATED"))
        .body("quantityProductsInStock", is(8));

    verify(legacyGateway)
        .updateStoreOnLegacySystem(
            argThat(s -> s.id == id && "STORE-UPDATED".equals(s.name)));
  }

  @Test
  public void updateWithoutNameShouldBeRejected() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"quantityProductsInStock\":8}")
        .when()
        .put(PATH + "/1")
        .then()
        .statusCode(422);

    verifyNoInteractions(legacyGateway);
  }

  @Test
  public void updateUnknownStoreShouldReturnNotFound() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"GHOST\"}")
        .when()
        .put(PATH + "/9999")
        .then()
        .statusCode(404);

    verifyNoInteractions(legacyGateway);
  }

  @Test
  public void patchNameOnlyShouldKeepStock() {
    int id = createStore("STORE-PATCH-NAME", 5);

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"STORE-PATCHED\"}")
        .when()
        .patch(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("name", is("STORE-PATCHED"))
        .body("quantityProductsInStock", is(5));

    verify(legacyGateway)
        .updateStoreOnLegacySystem(
            argThat(s -> s.id == id && s.quantityProductsInStock == 5));
  }

  @Test
  public void patchStockOnlyShouldKeepName() {
    int id = createStore("STORE-PATCH-STOCK", 5);

    given()
        .contentType(ContentType.JSON)
        .body("{\"quantityProductsInStock\":9}")
        .when()
        .patch(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("name", is("STORE-PATCH-STOCK"))
        .body("quantityProductsInStock", is(9));
  }

  @Test
  public void patchUnknownStoreShouldReturnNotFound() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"GHOST\"}")
        .when()
        .patch(PATH + "/9999")
        .then()
        .statusCode(404);

    verifyNoInteractions(legacyGateway);
  }

  @Test
  public void getAndDeleteShouldWork() {
    int id = createStore("STORE-DELETE", 1);

    given().when().get(PATH + "/" + id).then().statusCode(200).body("name", is("STORE-DELETE"));
    given().when().delete(PATH + "/" + id).then().statusCode(204);
    given().when().get(PATH + "/" + id).then().statusCode(404);
    given().when().delete(PATH + "/" + id).then().statusCode(404);
  }

  @Test
  public void listShouldContainSeedStores() {
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"), containsString("KALLAX"));
  }

  private int createStore(String name, int stock) {
    return given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"" + name + "\",\"quantityProductsInStock\":" + stock + "}")
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .extract()
        .path("id");
  }
}
