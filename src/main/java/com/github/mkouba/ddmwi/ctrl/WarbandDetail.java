package com.github.mkouba.ddmwi.ctrl;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.RestResponse;

import com.github.mkouba.ddmwi.CreatureView;
import com.github.mkouba.ddmwi.Warband;
import com.github.mkouba.ddmwi.dao.CreatureDao;
import com.github.mkouba.ddmwi.dao.Filters;
import com.github.mkouba.ddmwi.dao.PageResults;
import com.github.mkouba.ddmwi.dao.SortInfo;
import com.github.mkouba.ddmwi.dao.WarbandDao;
import com.github.mkouba.ddmwi.security.UserIdentityProvider;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.qute.Qute;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.persistence.NoResultException;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@NonBlocking
@Path(WarbandDetail.PATH)
public class WarbandDetail extends Controller {

    static final String PATH = "/warband-detail";

    @Inject
    WarbandDao warbandDao;

    @Inject
    CreatureDao creatureDao;

    @Inject
    CurrentIdentityAssociation identity;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Uni<TemplateInstance> get() {
        return toUni(Templates.warband(new Warband(),
                new PageResults<>(Collections.emptyList(), 0, 0), Collections.emptyList(), "",
                new SortInfo(creatureDao.getSortOptions())));
    }

    @POST
    public Uni<RestResponse<Object>> create(@BeanParam WarbandForm form) {
        URI listUri = uriFrom(WarbandList.PATH);
        Uni<Warband> warband = form.applyTo(new Warband(), false).chain(
                w -> identity.getDeferredIdentity().chain(
                        i -> Panache.withTransaction(
                                () -> w.setUser(i.getAttribute(UserIdentityProvider.USER_ID)).persistAndFlush())));
        return warband.onItem().transform(w -> RestResponse.seeOther(listUri))
                .onFailure()
                .recoverWithUni(t -> {
                    return recoverWithNewSession(() -> failureToResponse(t,
                            messages -> form.applyTo(new Warband(), false).map(w -> Templates.warband(w,
                                    PageResults.empty(), messages, "", new SortInfo(creatureDao.getSortOptions()))),
                            cause -> Qute.fmt("Warband with name \"{}\" already exists", form.name)));
                });
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.TEXT_HTML)
    public Uni<TemplateInstance> get(Long id, @RestQuery String q, @RestQuery int page, @RestQuery String sortBy) {
        SortInfo sortInfo = new SortInfo(sortBy, creatureDao.getSortOptions());
        return warbandDao.findWarband(id).chain(w -> warbandDao.findWarbandCreatures(w, creatureDao.parse(q), page, sortInfo)
                .map(pr -> Templates.warband(w, pr, List.of(), q, sortInfo))).onFailure().recoverWithItem(t -> {
                    if (t instanceof NoResultException) {
                        return Templates.error("Warband not found.", false);
                    }
                    return Templates.error();
                });
    }

    @WithTransaction
    @GET
    @Path("{id}/available-creatures")
    @Produces(MediaType.TEXT_HTML)
    public Uni<TemplateInstance> availableCreatures(Long id, @RestQuery String q, @RestQuery int page,
            @RestQuery String sortBy) {
        SortInfo sortInfo = new SortInfo(sortBy, creatureDao.getSortOptions());
        return warbandDao.findWarband(id)
                .chain(w -> warbandDao.findWarbandCreatures(w, creatureDao.parse(q), page, sortInfo).map(pr -> {
                    setHtmxPush("/warband-detail/%s?q=%s&sortBy=%s&page=%s", id, q, sortBy, page);
                    return Tags.creatureCards(w, pr, q, sortInfo, "/warband-detail/" + id + "/available-creatures",
                            "#warband-available-creatures");
                }))
                .onFailure().recoverWithItem(t -> {
                    if (t instanceof NoResultException) {
                        return Templates.error("Warband not found .", false);
                    }
                    return Templates.error();
                });
    }

    @WithTransaction
    @POST
    @Path("{id}")
    public Uni<RestResponse<Object>> update(@RestPath Long id, @BeanParam WarbandForm form) {
        URI detailUri = uriInfo.getRequestUri();
        return warbandDao.findWarband(id)
                .onItem().ifNotNull().call(form::applyTo)
                .onItem().ifNotNull().transform(v -> RestResponse.seeOther(detailUri))
                // TODO error page
                .onItem().ifNull().continueWith(RestResponse.notFound())
                .onFailure().recoverWithUni(t -> {
                    return recoverWithNewSession(() -> warbandDao.findWarband(id).chain(w -> {
                        return failureToResponse(t,
                                messages -> {
                                    return form.applyTo(w, false)
                                            .chain(fw -> warbandDao.findWarbandCreatures(fw, Filters.EMPTY, 0,
                                                    new SortInfo(creatureDao.getSortOptions())).map(wc -> {
                                                        return Templates.warband(fw, wc, messages, "", null);
                                                    }));
                                },
                                cause -> Qute.fmt("Warband with name \"{}\" already exists", form.name));
                    }));
                });
    }

    @WithTransaction
    @POST
    @Path("{id}/add-creature/{creatureId}")
    public Uni<RestResponse<Object>> addCreature(@RestPath Long id, @RestPath Long creatureId, @RestForm String queryStr) {
        URI requestUri = uriFrom(PATH + "/" + id, queryStr);
        // TODO check user
        return creatureDao.findCreature(creatureId).onItem().ifNotNull()
                .transformToUni(c -> warbandDao.findWarband(id).onItem().ifNotNull().invoke(w -> w.addCreature(c))).onItem()
                .ifNotNull().transform(e -> RestResponse.seeOther(requestUri))
                .onItem().ifNull().continueWith(RestResponse.notFound());
    }

    @WithTransaction
    @POST
    @Path("{id}/remove-creature/{warbandCreatureId}")
    public Uni<RestResponse<Object>> removeCreature(@RestPath Long id, @RestPath Long warbandCreatureId,
            @RestForm String queryStr) {
        URI requestUri = uriFrom(PATH + "/" + id, queryStr);
        // TODO check user
        return warbandDao.findWarband(id).onItem().ifNotNull().invoke(w -> {
            w.removeCreature(warbandCreatureId);
        }).onItem().ifNotNull().transform(e -> RestResponse.seeOther(requestUri))
                .onItem().ifNull().continueWith(RestResponse.notFound());
    }

    @WithTransaction
    @POST
    @Path("{id}/delete")
    public Uni<RestResponse<Object>> delete(@RestPath Long id) {
        URI listUri = uriFrom(WarbandList.PATH);
        return Warband.deleteById(id).map(result -> RestResponse.seeOther(listUri));
    }

    @WithTransaction
    @POST
    @Path("{id}/move-left/{warbandCreatureId}")
    public Uni<TemplateInstance> moveLeft(@RestPath Long id, @RestPath Long warbandCreatureId) {
        SortInfo sortInfo = new SortInfo(null, creatureDao.getSortOptions());
        PageResults<CreatureView> page = PageResults.empty();
        return warbandDao.findWarband(id).map(w -> {
            w.moveLeft(warbandCreatureId);
            return Templates.warband$warbandCreatures(w, page, null, sortInfo);
        });
    }

    @WithTransaction
    @POST
    @Path("{id}/move-right/{warbandCreatureId}")
    public Uni<TemplateInstance> moveRight(@RestPath Long id, @RestPath Long warbandCreatureId) {
        SortInfo sortInfo = new SortInfo(null, creatureDao.getSortOptions());
        PageResults<CreatureView> page = PageResults.empty();
        return warbandDao.findWarband(id).map(w -> {
            w.moveRight(warbandCreatureId);
            return Templates.warband$warbandCreatures(w, page, null, sortInfo);
        });
    }

}
