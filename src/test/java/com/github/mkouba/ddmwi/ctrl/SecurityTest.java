package com.github.mkouba.ddmwi.ctrl;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

@QuarkusTest
public class SecurityTest extends ControllerTest {

    @RunOnVertxContext
    @Test
    public void testLoginRedirect(UniAsserter asserter) {
        asserter.execute(() -> executeBlocking(() -> {
            assertLoginPageRedirect("/creature-list");
            assertLoginPageRedirect("/creature-detail/1");
            assertLoginPageRedirect("/warband-list");
            assertLoginPageRedirect("/warband-detail/1");
            assertLoginPageRedirect("/creatures/import");
            assertLoginPageRedirect("/users");
            assertLoginPageRedirect("/collection/import");
            assertPageForbidden("/creature-detail/1", "foo", "foo");

            String html = given()
                    .auth()
                    .form("foo", "foo", formAuthConfig())
                    .when()
                    .get("/creature-list")
                    .then()
                    .statusCode(200)
                    .contentType("text/html")
                    .extract().response().asString();
            assertTrue(html.contains("<h1>Creatures"), html);
        }));
    }

    private void assertLoginPageRedirect(String path) {
        String html = given()
                .when()
                .get(path)
                .then()
                .statusCode(200)
                .contentType("text/html")
                .extract().response().asString();
        assertTrue(html.contains("<h1>Sign in"), html);
    }

    private void assertPageForbidden(String path, String username, String password) {
        given().auth()
                .form(username, password, formAuthConfig())
                .when()
                .get(path)
                .then()
                //.log().all()
                .statusCode(403);
    }

}
