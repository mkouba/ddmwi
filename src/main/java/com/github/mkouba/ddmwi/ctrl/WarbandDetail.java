package com.github.mkouba.ddmwi.ctrl;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.RestResponse;

import com.github.mkouba.ddmwi.Warband;
import com.github.mkouba.ddmwi.dao.CreatureDao;
import com.github.mkouba.ddmwi.dao.Filters;
import com.github.mkouba.ddmwi.dao.PageResults;
import com.github.mkouba.ddmwi.dao.SortInfo;
import com.github.mkouba.ddmwi.dao.WarbandDao;
import com.github.mkouba.ddmwi.security.UserIdentityProvider;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.qute.Qute;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Uni;

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
    public TemplateInstance get() {
        return Templates.warband(Uni.createFrom().item(new Warband()),
                Uni.createFrom().item(new PageResults<>(Collections.emptyList(), 0, 0)), Collections.emptyList(), "",
                new SortInfo(creatureDao.getSortOptions()));
    }

    @POST
    public Uni<RestResponse<Object>> create(@BeanParam WarbandForm form) {
        URI listUri = uriFrom(WarbandList.PATH);
        Uni<Warband> warband = form.applyTo(new Warband(), false).chain(
                w -> identity.getDeferredIdentity().chain(
                        i -> Panache.withTransaction(
                                () -> w.setUser(i.getAttribute(UserIdentityProvider.USER_ID)).persistAndFlush())));
        return warband.onItem().transform(v -> RestResponse.seeOther(listUri))
                .onFailure()
                .recoverWithUni(t -> {
                    HibernateReactivePanache.destroySession();
                    return failureToResponse(t,
                            messages -> Templates.warband(warband,
                                    Uni.createFrom().item(new PageResults<>(Collections.emptyList(), 0, 0)), messages, "",
                                    new SortInfo(creatureDao.getSortOptions())),
                            cause -> Qute.fmt("Warband with name \"{}\" already exists", form.name));
                });
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get(Long id, @RestQuery String q, @RestQuery int page, @RestQuery String sortBy) {
        SortInfo sortInfo = new SortInfo(sortBy, creatureDao.getSortOptions());
        Uni<Warband> warband = warbandDao.findWarband(id).memoize().indefinitely();
        // TODO warband not found
        return Templates.warband(warband,
                warbandDao.findWarbandCreatures(warband, creatureDao.parse(q), page, sortInfo).memoize().indefinitely(),
                List.of(),
                q,
                sortInfo);
    }

    @GET
    @Path("{id}/available-creatures")
    @Produces(MediaType.TEXT_HTML)
    public Uni<TemplateInstance> availableCreatures(Long id, @RestQuery String q, @RestQuery int page,
            @RestQuery String sortBy) {
        SortInfo sortInfo = new SortInfo(sortBy, creatureDao.getSortOptions());
        return warbandDao.findWarband(id)
                .onItem().transform(w -> {
                    setHtmxPush("/warband-detail/%s?q=%s&sortBy=%s&page=%s", id, q, sortBy, page);
                    return Tags.creatureCards(w,
                            warbandDao.findWarbandCreatures(w, creatureDao.parse(q), page, sortInfo).memoize().indefinitely(),
                            q, sortInfo, "/warband-detail/" + id + "/available-creatures", "#warband-available-creatures");
                })
                .onFailure().recoverWithItem(t -> {
                    if (t instanceof NoResultException) {
                        return Templates.error("Warband not found .", false);
                    }
                    return Templates.error();
                });
    }

    @POST
    @Path("{id}")
    public Uni<RestResponse<Object>> update(@RestPath Long id, @BeanParam WarbandForm form) {
        URI detailUri = uriInfo.getRequestUri();
        return Panache.withTransaction(() -> warbandDao.findWarband(id)
                .onItem().ifNotNull().call(form::applyTo))
                .onItem().ifNotNull().transform(v -> RestResponse.seeOther(detailUri))
                // TODO error page
                .onItem().ifNull().continueWith(RestResponse.notFound())
                .onFailure().recoverWithUni(t -> {
                    HibernateReactivePanache.destroySession();
                    return warbandDao.findWarband(id).flatMap(w -> {
                        return failureToResponse(t,
                                messages -> {
                                    Uni<Warband> warband = form.applyTo(w, false).memoize().indefinitely();
                                    return Templates.warband(warband,
                                            warbandDao.findWarbandCreatures(warband, Filters.EMPTY, 0,
                                                    new SortInfo(creatureDao.getSortOptions()))
                                                    .memoize().indefinitely(),
                                            messages, "",
                                            null);
                                },
                                cause -> Qute.fmt("Warband with name \"{}\" already exists", form.name));
                    });
                });
    }

    @POST
    @Path("{id}/add-creature/{creatureId}")
    public Uni<RestResponse<Object>> addCreature(@RestPath Long id, @RestPath Long creatureId, @RestForm String queryStr) {
        URI requestUri = uriFrom(PATH + "/" + id, queryStr);
        // TODO check user
        return Panache.withTransaction(() -> {
            return creatureDao.findCreature(creatureId).onItem().ifNotNull()
                    .transformToUni(c -> warbandDao.findWarband(id).onItem().ifNotNull().invoke(w -> w.addCreature(c))).onItem()
                    .ifNotNull().transform(e -> RestResponse.seeOther(requestUri))
                    .onItem().ifNull().continueWith(RestResponse.notFound());
        });
    }

    @POST
    @Path("{id}/remove-creature/{warbandCreatureId}")
    public Uni<RestResponse<Object>> removeCreature(@RestPath Long id, @RestPath Long warbandCreatureId,
            @RestForm String queryStr) {
        URI requestUri = uriFrom(PATH + "/" + id, queryStr);
        // TODO check user
        return Panache.withTransaction(() -> {
            return warbandDao.findWarband(id).onItem().ifNotNull().invoke(w -> {
                w.removeCreature(warbandCreatureId);
            }).onItem().ifNotNull().transform(e -> RestResponse.seeOther(requestUri))
                    .onItem().ifNull().continueWith(RestResponse.notFound());
        });
    }

    @POST
    @Path("{id}/delete")
    public Uni<RestResponse<Object>> delete(@RestPath Long id) {
        URI listUri = uriFrom(WarbandList.PATH);
        return Panache.withTransaction(() -> Warband.deleteById(id)).map(result -> RestResponse.seeOther(listUri));
    }

}
