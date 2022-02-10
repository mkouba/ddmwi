package com.github.mkouba.ddmwi.ctrl;

import java.net.URI;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.resteasy.reactive.RestResponse;

import io.quarkus.qute.Results;
import io.quarkus.qute.TemplateExtension;

@Path("/")
public class Index {

    @Inject
    UriInfo uriInfo;

    @GET
    public RestResponse<Object> index() {
        URI warbandsUri = uriInfo.getRequestUriBuilder().replacePath("/warband-list").build();
        return RestResponse.seeOther(warbandsUri);
    }

    @TemplateExtension(namespace = "app")
    static Object version() {
        Optional<String> val = ConfigProvider.getConfig().getOptionalValue("quarkus.application.version", String.class);
        return val.isPresent() ? val.get() : Results.NotFound.from("quarkus.application.version");
    }

}
