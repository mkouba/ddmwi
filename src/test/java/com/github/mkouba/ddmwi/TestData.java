package com.github.mkouba.ddmwi;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.github.mkouba.ddmwi.User.Role;
import com.github.mkouba.ddmwi.ctrl.HibernateReactivePanache;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;

public class TestData {

    public static Uni<Void> init() {
        // TODO insert all
        return performPersist(UserTest.create("foo", "foo", Role.USER),
                UserTest.create("admin", "admin", Role.ADMIN));
    }

    public static Uni<Void> delete() {
        // TODO delete all
        return perform(() -> {
            return User.delete("username in :names", Map.of("names", List.of("foo", "admin")));
        }).replaceWithVoid();
    }

    public static <T> Uni<T> perform(Supplier<Uni<T>> work) {
        return HibernateReactivePanache.withTransaction(work);
    }

    @SuppressWarnings("unchecked")
    public static <T extends PanacheEntityBase> Uni<Void> performPersist(T... entities) {
        return perform(() -> persist(entities));
    }

    @SuppressWarnings("unchecked")
    public static <T extends PanacheEntityBase> Uni<Void> persist(T... entities) {
        return Panache.getSession().chain(session -> session.persistAll((Object[]) entities));
    }
}
