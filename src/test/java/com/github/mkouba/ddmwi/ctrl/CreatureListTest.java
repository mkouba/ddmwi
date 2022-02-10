package com.github.mkouba.ddmwi.ctrl;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.mkouba.ddmwi.Creature;
import com.github.mkouba.ddmwi.CreatureTest;
import com.github.mkouba.ddmwi.TestData;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

@QuarkusTest
public class CreatureListTest extends ControllerTest {

    @BeforeAll
    static void init() {
        TestData.performPersist(CreatureTest.creature("Rask")
                .good().cost(50).attr(c -> c.ac = 22).civilization().wild().done(),
                CreatureTest.creature("Goristro")
                        .evil().cost(60).underdark().done())
                .await()
                .atMost(Duration.ofSeconds(5));
    }

    @AfterAll
    static void delete() {
        TestData.perform(() -> Creature.delete("name in (:names)", Map.of("names", List.of("Rask", "Goristro"))))
                .await()
                .atMost(Duration.ofSeconds(5));
    }

    @RunOnVertxContext
    @Test
    public void testList(UniAsserter asserter) {
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
    }

}
