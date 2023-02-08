package com.github.mkouba.ddmwi.ctrl;

import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.resteasy.reactive.RestResponse;

import io.quarkus.qute.Results;
import io.quarkus.qute.TemplateExtension;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/")
public class Index extends Controller {

    @GET
    public RestResponse<Object> index() {
        return RestResponse.seeOther(uriFrom(WarbandList.PATH));
    }

    @TemplateExtension(namespace = "app")
    static Object version() {
        Optional<String> val = ConfigProvider.getConfig().getOptionalValue("quarkus.application.version", String.class);
        return val.isPresent() ? val.get() : Results.NotFound.from("quarkus.application.version");
    }

}
