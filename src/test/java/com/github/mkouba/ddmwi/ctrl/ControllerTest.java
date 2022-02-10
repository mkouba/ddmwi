package com.github.mkouba.ddmwi.ctrl;

import java.time.Duration;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import com.github.mkouba.ddmwi.TestData;

import io.restassured.authentication.FormAuthConfig;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;

public abstract class ControllerTest {

    @Inject
    Vertx vertx;

    @BeforeAll
    protected static void initTestData() {
        TestData.init().await().atMost(Duration.ofSeconds(5));
    }

    @AfterAll
    protected static void deleteTestData() {
        TestData.delete().await().atMost(Duration.ofSeconds(5));
    }

    protected Uni<Void> executeBlocking(Runnable action) {
        return vertx.executeBlocking(Uni.createFrom().item(() -> {
            action.run();
            return true;
        }).replaceWithVoid());
    }

    protected FormAuthConfig formAuthConfig() {
        return new FormAuthConfig("/login_security_check", "username", "password");
        //.withLoggingEnabled();
    }

}
