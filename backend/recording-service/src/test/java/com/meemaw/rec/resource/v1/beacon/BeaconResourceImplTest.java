package com.meemaw.rec.resource.v1.beacon;

import static com.meemaw.test.matchers.SameJSON.sameJson;
import static io.restassured.RestAssured.given;

import com.meemaw.test.testconainers.Postgres;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.ws.rs.core.MediaType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;


@Postgres
@QuarkusTest
@Tag("integration")
public class BeaconResourceImplTest {

  @Test
  public void postBeacon_shouldThrowError_whenEmptyTextPlainPayload() {
    given()
        .when().contentType(MediaType.TEXT_PLAIN).post(BeaconResource.PATH)
        .then()
        .statusCode(422)
        .body(sameJson(
            "{\"error\":{\"statusCode\":422,\"reason\":\"Unprocessable Entity\",\"message\":\"No content to map due to end-of-input\"}}"));
  }

  @Test
  public void postBeacon_shouldThrowError_whenEmptyJsonPayload() {
    given()
        .when().contentType(MediaType.APPLICATION_JSON).post(BeaconResource.PATH)
        .then()
        .statusCode(422)
        .body(sameJson(
            "{\"error\":{\"statusCode\":422,\"reason\":\"Unprocessable Entity\",\"message\":\"No content to map due to end-of-input\"}}"));
  }

  @Test
  public void postBeacon_shouldThrowError_whenEmptyJson() {
    given()
        .when().contentType(MediaType.APPLICATION_JSON).body("{}").post(BeaconResource.PATH)
        .then()
        .statusCode(400)
        .body(sameJson(
            "{\"error\":{\"statusCode\":400,\"reason\":\"Bad Request\",\"message\":\"Validation Error\",\"errors\":{\"sequence\":\"s must be greater than 0\",\"events\":\"e may not be null\"}}}"));
  }

  @Test
  public void postBeaconShouldStore_whenValidPayload() throws IOException, URISyntaxException {
    String payload = Files.readString(Path.of(getClass().getResource(
        "/beacon/initial.json").toURI()));

    given()
        .when().contentType(ContentType.JSON).body(payload).post(BeaconResource.PATH)
        .then()
        .statusCode(200)
        .body(sameJson("{\"data\":{\"timestamp\":400,\"sequence\":1,\"events\":[]}}"));
  }

}
