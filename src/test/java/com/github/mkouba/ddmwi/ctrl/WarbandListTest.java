package com.github.mkouba.ddmwi.ctrl;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.mkouba.ddmwi.Warband;
import com.github.mkouba.ddmwi.WarbandTest;

import io.quarkus.test.hibernate.reactive.panache.TransactionalUniAsserter;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;

@QuarkusTest
public class WarbandListTest extends ControllerTest {

    @RunOnVertxContext
    @Test
    public void testList(TransactionalUniAsserter asserter) {
        asserter
                .execute(() -> createUsers(fooUser -> WarbandTest.create(fooUser, "Testik").persist()
                        .chain(w1 -> WarbandTest.create(fooUser, "Boom").persist().replaceWithVoid())));
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
            assertTrue(html.contains("Warbands"), html);
            assertTrue(html.contains("Testik"));
            assertTrue(html.contains("Boom"));
        }));
        asserter.execute(() -> Warband.deleteAll());
        asserter.execute(this::deleteUsers);
    }

}
