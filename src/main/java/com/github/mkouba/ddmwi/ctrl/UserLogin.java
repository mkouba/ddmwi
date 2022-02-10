package com.github.mkouba.ddmwi.ctrl;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.NonBlocking;

@NonBlocking
@Path("/login")
public class UserLogin extends Controller {

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance login() {
        return Templates.login();
    }

    @Path("error")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance loginError() {
        return Templates.error("Authentication failed!", true);
    }

}
