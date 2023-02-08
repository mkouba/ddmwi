package com.github.mkouba.ddmwi.ctrl;

import java.util.function.Function;

import com.github.mkouba.ddmwi.User;
import com.github.mkouba.ddmwi.User.Role;
import com.github.mkouba.ddmwi.UserTest;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.restassured.authentication.FormAuthConfig;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import jakarta.inject.Inject;

public abstract class ControllerTest {

    @Inject
    Vertx vertx;

    <T extends PanacheEntityBase> Uni<Void> createUsers() {
        return createUsers(foo -> Uni.createFrom().voidItem());
    }

    Uni<Void> createUsers(Function<User, Uni<Void>> fooUserConsumer) {
        return UserTest.create("foo", "foo", Role.USER).<User> persist().call(u -> fooUserConsumer.apply(u))
                .chain(fu -> UserTest.create("admin", "admin", Role.ADMIN).persist()).replaceWithVoid();
    }

    Uni<Long> deleteUsers() {
        return User.deleteAll();
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
