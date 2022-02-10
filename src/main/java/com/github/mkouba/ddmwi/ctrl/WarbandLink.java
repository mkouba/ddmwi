package com.github.mkouba.ddmwi.ctrl;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.github.mkouba.ddmwi.dao.WarbandDao;

import io.quarkus.qute.TemplateInstance;
import io.smallrye.mutiny.Uni;

@Path("/warband-link")
public class WarbandLink extends Controller {

    @Inject
    WarbandDao warbandDao;

    @GET
    @Path("{id}")
    @Produces(MediaType.TEXT_HTML)
    public Uni<TemplateInstance> get(Long id) {
        return warbandDao.findWarbandLink(id)
                .onItem().transform(w -> Templates.warbandLink(w))
                .onFailure().recoverWithItem(t -> {
                    if (t instanceof NoResultException) {
                        return Templates.error("Warband not found or is not publicly available.", false);
                    }
                    return Templates.error();
                });
    }

}
