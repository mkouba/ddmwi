package com.github.mkouba.ddmwi.ctrl;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.mkouba.ddmwi.WarbandTest;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

@QuarkusTest
public class WarbandListTest extends ControllerTest {

    @RunOnVertxContext
    @Test
    public void testList(UniAsserter asserter) {
        asserter
                .execute(() -> persistAll(u -> List.of(WarbandTest.create(u, "Testik"), WarbandTest.create(u, "Boom"))))
                .execute(() -> executeBlocking(() -> {
                    String html = given()
                            .auth()
                            .form("foo", "foo", formAuthConfig())
                            .when()
                            .get("/warband-list")
                            .then()
                            .statusCode(200)
                            .contentType("text/html")
                            .extract().response().asString();
                    assertTrue(html.contains("Your warbands"), html);
                    assertTrue(html.contains("Testik"));
                    assertTrue(html.contains("Boom"));
                }))
                .execute(this::deleteAll);
    }

}
