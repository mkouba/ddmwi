package com.github.mkouba.ddmwi.security;

import com.github.mkouba.ddmwi.User.Role;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.impl.LazyValue;
import io.quarkus.qute.TemplateExtension;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;

@TemplateExtension(namespace = "user")
public class CurrentIdentity {

    private static final Uni<Boolean> FALSE = Uni.createFrom().item(false);

    private static LazyValue<ArcContainer> container = new LazyValue<>(Arc::container);

    static Uni<Boolean> signedIn() {
        ArcContainer arc = container.get();
        return arc.requestContext().isActive() ? getDeferredIdentity(arc).map(i -> !i.isAnonymous()) : FALSE;
    }

    static Uni<Boolean> isAdmin() {
        ArcContainer arc = container.get();
        return arc.requestContext().isActive()
                ? getDeferredIdentity(arc).map(i -> !i.isAnonymous() && i.hasRole(Role.ADMIN_STR))
                : FALSE;
    }

    static Uni<String> name() {
        ArcContainer arc = container.get();
        return arc.requestContext().isActive()
                ? getDeferredIdentity(arc).map(i -> i.isAnonymous() ? "-" : i.getPrincipal().getName())
                : Uni.createFrom().nullItem();
    }

    private static Uni<SecurityIdentity> getDeferredIdentity(ArcContainer arc) {
        return arc.instance(CurrentIdentityAssociation.class).get().getDeferredIdentity();
    }

}
