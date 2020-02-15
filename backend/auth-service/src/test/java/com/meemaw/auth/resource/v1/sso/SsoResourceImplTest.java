package com.meemaw.auth.resource.v1.sso;

import static com.meemaw.test.matchers.SameJSON.sameJson;
import static io.restassured.RestAssured.given;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.meemaw.auth.datasource.user.UserDatasource;
import com.meemaw.auth.model.sso.SsoSession;
import com.meemaw.auth.model.user.UserDTO;
import com.meemaw.auth.resource.v1.signup.SignupResourceImplTest;
import com.meemaw.shared.rest.response.DataResponse;
import com.meemaw.test.rest.mappers.JacksonMapper;
import com.meemaw.test.testconainers.Postgres;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.Cookie;
import io.restassured.response.Response;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Postgres
@QuarkusTest
@Tag("integration")
public class SsoResourceImplTest {

  @Inject
  MockMailbox mailbox;

  @Inject
  UserDatasource userDatasource;

  @BeforeEach
  void init() {
    mailbox.clear();
  }


  @Test
  public void login_should_fail_when_invalid_contentType() {
    given()
        .when()
        .contentType(MediaType.TEXT_PLAIN).post(SsoResource.PATH + "/login")
        .then()
        .statusCode(415)
        .body(sameJson(
            "{\"error\":{\"statusCode\":415,\"reason\":\"Unsupported Media Type\",\"message\":\"Media type not supported.\"}}"));
  }

  @Test
  public void login_should_fail_when_no_payload() {
    given()
        .when()
        .contentType(MediaType.APPLICATION_FORM_URLENCODED).post(SsoResource.PATH + "/login")
        .then()
        .statusCode(400)
        .body(sameJson(
            "{\"error\":{\"statusCode\":400,\"reason\":\"Bad Request\",\"message\":\"Validation Error\",\"errors\":{\"arg1\":\"Required\",\"arg0\":\"Email is required\"}}}"));
  }

  @Test
  public void login_should_fail_when_invalid_payload() {
    given()
        .when()
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .param("email", "random")
        .param("password", "random")
        .post(SsoResource.PATH + "/login")
        .then()
        .statusCode(400)
        .body(sameJson(
            "{\"error\":{\"statusCode\":400,\"reason\":\"Bad Request\",\"message\":\"Validation Error\",\"errors\":{\"arg1\":\"Password must be at least 8 characters long\",\"arg0\":\"must be a well-formed email address\"}}}"));
  }

  @Test
  public void login_should_fail_when_invalid_credentials() {
    given()
        .when()
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .param("email", "test@gmail.com")
        .param("password", "superFancyPassword")
        .post(SsoResource.PATH + "/login")
        .then()
        .statusCode(400)
        .body(sameJson(
            "{\"error\":{\"statusCode\":400,\"reason\":\"Bad Request\",\"message\":\"Invalid email or password\"}}"));
  }

  @Test
  public void logout_should_fail_when_no_cookie() {
    given()
        .when()
        .post(SsoResource.PATH + "/logout")
        .then()
        .statusCode(400)
        .body(sameJson(
            "{\"error\":{\"statusCode\":400,\"reason\":\"Bad Request\",\"message\":\"Validation Error\",\"errors\":{\"arg0\":\"SessionId cookie required\"}}}"));
  }

  @Test
  public void logout_should_clear_cookie_when_missing_cookie() {
    given()
        .when()
        .cookie(SsoSession.COOKIE_NAME, "random")
        .post(SsoResource.PATH + "/logout")
        .then()
        .statusCode(400)
        .body(sameJson(
            "{\"error\":{\"statusCode\":400,\"reason\":\"Bad Request\",\"message\":\"Session does not exist\"}}"))
        .cookie(SsoSession.COOKIE_NAME, "");
  }

  @Test
  public void session_should_fail_when_no_sessionId() {
    given()
        .when()
        .get(SsoResource.PATH + "/session")
        .then()
        .statusCode(400)
        .body(sameJson(
            "{\"error\":{\"statusCode\":400,\"reason\":\"Bad Request\",\"message\":\"Validation Error\",\"errors\":{\"arg0\":\"SessionId may not be blank\"}}}"));
  }

  @Test
  public void session_should_clear_session_cookie_when_missing_sessionId() {
    given()
        .when()
        .queryParam("id", "random")
        .get(SsoResource.PATH + "/session")
        .then()
        .statusCode(204)
        .cookie(SsoSession.COOKIE_NAME, "");
  }

  @Test
  public void me_should_fail_when_missing_sessionId_cookie() {
    given()
        .when()
        .get(SsoResource.PATH + "/me")
        .then()
        .statusCode(400)
        .body(sameJson(
            "{\"error\":{\"statusCode\":400,\"reason\":\"Bad Request\",\"message\":\"Validation Error\",\"errors\":{\"arg0\":\"SessionId may not be blank\"}}}"));
  }

  @Test
  public void me_should_clear_session_cookie_when_missing_sessionId() {
    given()
        .when()
        .cookie(SsoSession.COOKIE_NAME, "random")
        .queryParam("id", "random")
        .get(SsoResource.PATH + "/me")
        .then()
        .statusCode(204)
        .cookie(SsoSession.COOKIE_NAME, "");
  }

  @Test
  public void sso_flow_should_work_with_registered_user() throws JsonProcessingException {
    String email = "sso_flow_test@gmail.com";
    String password = "sso_flow_test_password";

    SignupResourceImplTest.signup(mailbox, email, password);
    String sessionId = login(email, password);

    UserDTO userDTO = userDatasource.findUserWithPasswordHash(email)
        .toCompletableFuture().join().orElseThrow().user();

    // should be able to get session by id
    given()
        .when()
        .queryParam("id", sessionId)
        .get(SsoResource.PATH + "/session")
        .then()
        .statusCode(200)
        .body(sameJson(JacksonMapper.get().writeValueAsString(DataResponse.data(userDTO))));

    // should be able to get session via cookie
    given()
        .when()
        .cookie(SsoSession.COOKIE_NAME, sessionId)
        .get(SsoResource.PATH + "/me")
        .then()
        .statusCode(200)
        .body(sameJson(JacksonMapper.get().writeValueAsString(DataResponse.data(userDTO))));

    // should be able to logout
    given()
        .when()
        .cookie(SsoSession.COOKIE_NAME, sessionId)
        .post(SsoResource.PATH + "/logout")
        .then()
        .statusCode(204)
        .cookie(SsoSession.COOKIE_NAME, "");
  }

  public static String login(String email, String password) {
    Response response = given()
        .when()
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .param("email", email)
        .param("password", password)
        .post(SsoResource.PATH + "/login");

    response.then().statusCode(204).cookie(SsoSession.COOKIE_NAME);
    Cookie cookie = response.getDetailedCookie(SsoSession.COOKIE_NAME);
    return cookie.getValue();
  }

}
