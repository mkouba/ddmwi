package com.github.mkouba.ddmwi.ctrl;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.mkouba.ddmwi.TestData;
import com.github.mkouba.ddmwi.User;
import com.github.mkouba.ddmwi.Warband;
import com.github.mkouba.ddmwi.WarbandTest;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

@QuarkusTest
public class WarbandListTest extends ControllerTest {

    @BeforeAll
    static void init() {
        TestData.perform(() -> User.<User> find("username", "foo").firstResult()
                .chain(u -> TestData.persist(WarbandTest.create(u, "Testik"), WarbandTest.create(u, "Boom"))))
                .await()
                .atMost(Duration.ofSeconds(5));
    }

    @AfterAll
    static void delete() {
        TestData.perform(() -> Warband.delete("name in (:names)", Map.of("names", List.of("Testik", "Boom"))))
                .await().atMost(Duration.ofSeconds(5));
    }

    @RunOnVertxContext
    @Test
    public void testList(UniAsserter asserter) {
        // user foo ans warbands Testik and Boom inserted in script
        asserter.execute(() -> executeBlocking(() -> {
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
        }));
    }

}
