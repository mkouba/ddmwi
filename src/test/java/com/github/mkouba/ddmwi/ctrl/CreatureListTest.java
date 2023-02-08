package com.github.mkouba.ddmwi.ctrl;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.mkouba.ddmwi.Creature;
import com.github.mkouba.ddmwi.CreatureTest;
import com.github.mkouba.ddmwi.TransactionUniAsserterInterceptor;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

@QuarkusTest
public class CreatureListTest extends ControllerTest {

    @RunOnVertxContext
    @Test
    public void testList(UniAsserter asserter) {
        asserter = new TransactionUniAsserterInterceptor(asserter);
        asserter.execute(() -> createUsers(fooUser -> CreatureTest.creature("Rask")
                .good().cost(50).attr(c -> c.ac = 22).civilization().wild().build().persist()
                .chain(c1 -> CreatureTest.creature("Goristro")
                        .evil().cost(60).underdark().build().persist().replaceWithVoid())));
        asserter.execute(() -> executeBlocking(() -> {
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
        }));
        asserter.execute(() -> Creature.deleteAll());
        asserter.execute(this::deleteUsers);
    }

}
