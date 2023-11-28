package com.github.mkouba.ddmwi.ctrl;

import com.github.mkouba.ddmwi.dao.WarbandDao;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.persistence.NoResultException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/warband-link")
public class WarbandLink extends Controller {

    @Inject
    WarbandDao warbandDao;

    @WithTransaction
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
