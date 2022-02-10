package com.github.mkouba.ddmwi.ctrl;

import java.net.URI;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.RestResponse;

import io.quarkus.logging.Log;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerResponse;

@Path("/logout")
public class UserLogout extends Controller {

    @ConfigProperty(name = "quarkus.http.auth.form.cookie-name")
    String cookieName;

    @Inject
    CurrentIdentityAssociation identity;

    @POST
    public Uni<RestResponse<Object>> logout(HttpServerResponse response) {
        URI loginUri = uriInfo.getRequestUriBuilder().replacePath("/login").build();
        return identity.getDeferredIdentity().map(identity -> {
            Log.infof("User %s logged out", identity.getPrincipal().getName());
            response.removeCookie(cookieName, true);
            return RestResponse.seeOther(loginUri);
        });
    }
}
