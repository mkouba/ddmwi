package com.github.mkouba.ddmwi.ctrl;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.mkouba.ddmwi.CreatureTest;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

@QuarkusTest
public class CreatureListTest extends ControllerTest {

    @RunOnVertxContext
    @Test
    public void testList(UniAsserter asserter) {
        asserter
                .execute(() -> persistAll(u -> List.of(
                        CreatureTest.creature("Rask")
                                .good().cost(50).attr(c -> c.ac = 22).civilization().wild().build(),
                        CreatureTest.creature("Goristro")
                                .evil().cost(60).underdark().build())))
                .execute(() -> executeBlocking(() -> {
                    String html = given()
                            .auth()
                            .form("foo", "foo", formAuthConfig())
                            .when()
                            .get("/creature-list")
                            .then()
                            .statusCode(200)
                            .contentType("text/html")
                            .extract().response().asString();
                    assertTrue(html.contains("Creatures"), html);
                    assertTrue(html.contains("Rask"));
                    assertTrue(html.contains("Goristro"));
                }))
                .execute(this::deleteAll);
    }

}
