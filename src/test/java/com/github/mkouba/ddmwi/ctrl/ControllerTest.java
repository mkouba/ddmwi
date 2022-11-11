package com.github.mkouba.ddmwi.ctrl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.inject.Inject;

import com.github.mkouba.ddmwi.User;
import com.github.mkouba.ddmwi.User.Role;
import com.github.mkouba.ddmwi.UserTest;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.restassured.authentication.FormAuthConfig;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;

public abstract class ControllerTest {

    @Inject
    Vertx vertx;

    private List<?> entities = null;

    <T extends PanacheEntityBase> Uni<Void> persistAll() {
        return persistAll(foo -> List.of());
    }

    @SuppressWarnings("unchecked")
    <T extends PanacheEntityBase> Uni<Void> persistAll(Function<User, List<T>> fun) {
        User foo = UserTest.create("foo", "foo", Role.USER);
        List<T> entities = fun.apply(foo);
        List<T> all = new ArrayList<>(entities.size() + 2);
        all.addAll(entities);
        all.add((T) foo);
        all.add((T) UserTest.create("admin", "admin", Role.ADMIN));
        this.entities = all;
        return Panache.withTransaction(() -> Panache.getSession().chain(s -> s.persistAll(all.toArray())));
    }

    Uni<Void> deleteAll() {
        if (entities == null) {
            return Uni.createFrom().voidItem();
        }
        List<?> all = new ArrayList<>(entities);
        this.entities = null;
        return Panache.withTransaction(() -> Panache.getSession().chain(s -> s.removeAll(all.toArray())));
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
