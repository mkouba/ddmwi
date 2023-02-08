package com.github.mkouba.ddmwi.ctrl;

import io.quarkus.qute.TemplateInstance;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path(UserLogin.PATH)
public class UserLogin extends Controller {

    static final String PATH = "/login";

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

}
