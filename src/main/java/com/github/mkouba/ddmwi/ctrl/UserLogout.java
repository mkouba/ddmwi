package com.github.mkouba.ddmwi.ctrl;

import java.net.URI;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;

import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@Path("/logout")
public class UserLogout extends Controller {

    private static final Logger LOG = Logger.getLogger(UserLogout.class);

    @ConfigProperty(name = "quarkus.http.auth.form.cookie-name")
    String cookieName;

    @Inject
    CurrentIdentityAssociation identity;

    @POST
    public Uni<RestResponse<Object>> logout(HttpServerResponse response) {
        URI loginUri = uriFrom(UserLogin.PATH);
        return identity.getDeferredIdentity().map(identity -> {
            LOG.infof("User %s logged out", identity.getPrincipal().getName());
            response.removeCookie(cookieName, true);
            return RestResponse.seeOther(loginUri);
        });
    }
}
