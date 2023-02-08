package com.github.mkouba.ddmwi.ctrl;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.RestResponse;

import com.github.mkouba.ddmwi.Creature;
import com.github.mkouba.ddmwi.CreaturePower;
import com.github.mkouba.ddmwi.dao.CreatureDao;
import com.github.mkouba.ddmwi.dao.WarbandDao;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.qute.Qute;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@NonBlocking
@Path(CreatureDetail.PATH)
public class CreatureDetail extends Controller {

    static final String PATH = "/creature-detail";

    @Inject
    WarbandDao warbandDao;

    @Inject
    CreatureDao creatureDao;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Uni<TemplateInstance> get() {
        return toUni(Templates.creature(new Creature()));
    }

    @POST
    public Uni<RestResponse<Object>> create(@BeanParam CreatureForm form) {
        URI listUri = uriFrom(CreatureList.PATH);
        Uni<Creature> creature = form.applyTo(new Creature(), false);
        return Panache.withTransaction(() -> creature.call(Creature::persistAndFlush))
                .onItem().transform(v -> RestResponse.seeOther(listUri))
                .onFailure()
                .recoverWithUni(t -> {
                    return recoverWithNewSession(() -> Panache.withSession(
                            () -> failureToResponse(t,
                                    messages -> form.applyTo(new Creature(), false).map(c -> Templates.creature(c, messages)),
                                    cause -> Qute.fmt("Creature with name \"{}\" already exists", form.name))));
                });
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.TEXT_HTML)
    public Uni<TemplateInstance> get(Long id) {
        return creatureDao.findCreature(id).map(c -> Templates.creature(c));
    }

    @POST
    @Path("{id}")
    public Uni<RestResponse<Object>> update(@RestPath Long id, @BeanParam CreatureForm form, @RestQuery Long warbandId) {
        URI detailUri = warbandId != null
                ? uriFrom(WarbandDetail.PATH + "/" + warbandId, "")
                : uriInfo.getRequestUri();

        return Panache.withTransaction(() -> creatureDao.findCreature(id)
                .onItem().ifNotNull().call(form::applyTo))
                // If successfull then reload the detail
                .onItem().ifNotNull().transform(v -> RestResponse.seeOther(detailUri))
                // TODO error page
                .onItem().ifNull().continueWith(RestResponse.notFound())
                .onFailure()
                .recoverWithUni(t -> {
                    return recoverWithNewSession(() -> Panache.withSession(
                            () -> failureToResponse(t,
                                    messages -> creatureDao.findCreature(id).chain(c -> form.applyTo(c, false))
                                            .map(c -> Templates.creature(
                                                    c,
                                                    messages)),
                                    cause -> Qute.fmt("Creature with name \"{}\" already exists", form.name))));
                });
    }

    @POST
    @Path("{id}/delete")
    public Uni<RestResponse<Object>> delete(@RestPath Long id) {
        URI listUri = uriFrom(CreatureList.PATH);
        return warbandDao.findRelevantWarbands(id)
                .chain(warbands -> warbands.isEmpty() ? deleteCreature(id, listUri) : cannotDeleteCreature(id));
    }

    @POST
    @Path("{creatureId}/add-power")
    public Uni<RestResponse<Object>> addPower(@RestPath Long creatureId, @BeanParam PowerForm form) {
        URI detailUri = uriFrom(CreatureDetail.PATH + "/" + creatureId);
        return Panache.withTransaction(() -> {
            CreaturePower power = new CreaturePower();
            power.creature = Creature.createDummy(creatureId);
            return form.apply(power).chain(p -> CreaturePower.persist(p));
        }).map((v -> RestResponse.seeOther(detailUri)));
    }

    @POST
    @Path("{creatureId}/delete-power/{powerId}")
    public Uni<RestResponse<Object>> deletePower(@RestPath Long creatureId, @RestPath Long powerId) {
        URI detailUri = uriFrom(CreatureDetail.PATH + "/" + creatureId);
        return Panache.withTransaction(() -> CreaturePower.delete("id = :id and creature.id = :creatureId",
                Map.of("id", powerId, "creatureId", creatureId))).map((v -> RestResponse.seeOther(detailUri)));
    }

    @POST
    @Path("{creatureId}/update-power/{powerId}")
    public Uni<RestResponse<Object>> updatePower(@RestPath Long creatureId, @RestPath Long powerId, @BeanParam PowerForm form) {
        URI detailUri = uriFrom(CreatureDetail.PATH + "/" + creatureId);
        return Panache.withTransaction(() -> CreaturePower.<CreaturePower> findById(powerId).chain(
                p -> form.apply(p)))
                .map((v -> RestResponse.seeOther(detailUri)));
    }

    Uni<RestResponse<Object>> cannotDeleteCreature(Long creatureId) {
        return creatureDao.findCreature(creatureId).chain(
                creature -> Templates
                        .creature(creature,
                                List.of(Qute.fmt("Creature with name \"{}\" is used in a warband", creature.name)))
                        .createUni()
                        .map(s -> RestResponse.ok(s, MediaType.TEXT_HTML_TYPE)));
    }

    Uni<RestResponse<Object>> deleteCreature(Long creatureId, URI listUri) {
        return Panache.withTransaction(() -> Creature.deleteById(creatureId)).map(result -> RestResponse.seeOther(listUri));
    }

}
