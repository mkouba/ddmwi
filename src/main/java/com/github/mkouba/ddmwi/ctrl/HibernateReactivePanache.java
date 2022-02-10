package com.github.mkouba.ddmwi.ctrl;

import java.util.function.Supplier;

import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;

public class HibernateReactivePanache {

    private static final Logger LOG = Logger.getLogger(HibernateReactivePanache.class);

    // FIXME https://github.com/quarkusio/quarkus/discussions/23166
    public static void destroySession() {
        Arc.container().instance(Mutiny.Session.class).destroy();
    }

    public static <T> Uni<T> withTransaction(Supplier<Uni<T>> work) {
        ManagedContext requestContext = Arc.container().requestContext();
        if (requestContext.isActive()) {
            LOG.debug("Request context active - skip activation: " + requestContext.getState().toString());
            return Panache.withTransaction(work);
        } else {
            LOG.debug("Activating request context");
            requestContext.activate();
            try {
                return Panache.withTransaction(work).onItemOrFailure().invoke(() -> {
                    LOG.debug("Terminating request context: " + requestContext.getState().toString());
                    requestContext.terminate();
                });
            } finally {
                requestContext.deactivate();
            }
        }
    }

}
