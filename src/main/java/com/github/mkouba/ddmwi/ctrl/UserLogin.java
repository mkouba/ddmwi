package com.github.mkouba.ddmwi.ctrl;

import java.net.URI;

import org.jboss.resteasy.reactive.RestResponse;

import com.github.mkouba.ddmwi.User.Role;

import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path(UserLogin.PATH)
public class UserLogin extends Controller {

    static final String PATH = "/login";

    @Inject
    CurrentIdentityAssociation identity;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Uni<TemplateInstance> login() {
        return toUni(Templates.login());
    }

    @Path("error")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Uni<TemplateInstance> loginError() {
        return toUni(Templates.error("Authentication failed!", true));
    }

    @Path("success")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Uni<RestResponse<Object>> loginSuccess() {
        URI warbandList = uriInfo.getRequestUriBuilder().replacePath("/warband-list").build();
        URI dashboard = uriInfo.getRequestUriBuilder().replacePath("/dashboard").build();
        return identity.getDeferredIdentity().map(i -> {
            if (!i.isAnonymous() && i.hasRole(Role.ADMIN_STR)) {
                return RestResponse.seeOther(dashboard);
            } else {
                return RestResponse.seeOther(warbandList);
            }
        });
    }

}
