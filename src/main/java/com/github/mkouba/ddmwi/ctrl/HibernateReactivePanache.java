package com.github.mkouba.ddmwi.ctrl;

import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.arc.Arc;

public class HibernateReactivePanache {

    // FIXME https://github.com/quarkusio/quarkus/discussions/23166
    public static void destroySession() {
        Arc.container().instance(Mutiny.Session.class).destroy();
    }

}
