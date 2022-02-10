package com.github.mkouba.ddmwi.ctrl;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.RestResponse.ResponseBuilder;

import com.github.mkouba.ddmwi.Creature;
import com.github.mkouba.ddmwi.Warband;
import com.github.mkouba.ddmwi.WarbandCreature;
import com.github.mkouba.ddmwi.Warband.PointLimit;
import com.github.mkouba.ddmwi.dao.CreatureDao;
import com.github.mkouba.ddmwi.dao.WarbandDao;
import com.github.mkouba.ddmwi.security.UserIdentityProvider;
import com.github.mkouba.ddmwi.dao.CreatureDao.CreatureName;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;

@Path("/warbands")
public class MyWarbands extends Controller {

    private static Logger LOG = Logger.getLogger(MyWarbands.class);

    @Inject
    Vertx vertx;

    @Inject
    CurrentIdentityAssociation identity;

    @Inject
    WarbandDao warbandDao;

    @Inject
    CreatureDao creatureDao;

    @GET
    @Path("/export")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<RestResponse<String>> exportWarbands() {
        return identity.getDeferredIdentity().chain(i -> warbandDao.findAllWarbands(i).chain(w -> warbandsToJson(i, w)));
    }

    @GET
    @Path("/export/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<RestResponse<String>> exportWarband(Long id) {
        return identity.getDeferredIdentity().chain(i -> warbandDao.findWarband(id).chain(w -> warbandsToJson(i, List.of(w))));
    }

    @GET
    @Path("/import")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get() {
        return Templates.warbandsImport();
    }

    @POST
    @Path("/import")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_HTML)
    public Uni<RestResponse<Object>> importWarbands(@MultipartForm FileImportForm form) {
        URI listUri = uriInfo.getRequestUriBuilder().replacePath("/warband-list").build();
        return importWarbandsJson(form).map(v -> RestResponse.seeOther(listUri));
    }

    Uni<RestResponse<String>> warbandsToJson(SecurityIdentity identity, List<Warband> warbands) {
        String filename = "warbands-" + identity.getPrincipal().getName() + "-"
                + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".json";
        JsonObject export = new JsonObject();
        export.put("user", identity.getPrincipal().getName());
        export.put("created", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        JsonArray all = new JsonArray();
        export.put("warbands", all);
        for (Warband warband : warbands) {
            all.add(warbandToJson(warband));
        }
        LOG.infof("Exported %s warbands of user [%s]", warbands.size(), identity.getPrincipal().getName());
        return Uni.createFrom().item(ResponseBuilder.ok(export.encodePrettily()).header("Content-Disposition",
                "attachment; filename=" + filename)
                .build());
    }

    static JsonObject warbandToJson(Warband warband) {
        JsonObject json = new JsonObject();
        json.put("name", warband.name);
        json.put("pointLimit", warband.pointLimit);
        json.put("arena", warband.arena);
        if (warband.note != null) {
            json.put("note", warband.note);
        }
        JsonArray creatures = new JsonArray();
        for (WarbandCreature c : warband.creatures) {
            creatures.add(new JsonObject().put("name", c.creature.name));
        }
        json.put("creatures", creatures);
        return json;
    }

    Uni<Void> importWarbandsJson(FileImportForm form) {
        java.nio.file.Path filePath = form.importFile.filePath();
        LOG.infof("Importing warbands from file: %s", filePath);
        return readFile(filePath).chain(str -> identity.getDeferredIdentity()
                .chain(i -> creatureDao.findAllCreatureNames().chain(cn -> Panache.withTransaction(
                        () -> warbandDao.findAllWarbands(i).chain(warbands -> updateWarbands(str, warbands, cn,
                                i.getAttribute(UserIdentityProvider.USER_ID)))))));
    }

    private Uni<Void> updateWarbands(String str, List<Warband> warbands, List<CreatureName> creatureNames, Long userId) {
        JsonObject jsonData = new JsonObject(str);
        JsonArray warbandsJson = jsonData.getJsonArray("warbands");
        List<Warband> all = new ArrayList<>();
        for (Object o : warbandsJson) {
            JsonObject json = (JsonObject) o;
            String name = json.getString("name");
            if (name == null || name.isEmpty()) {
                continue;
            }
            Optional<Warband> existing = warbands.stream().filter(w -> w.name.equals(name))
                    .findAny();
            Warband warband = existing.orElse(new Warband().setUser(userId));
            applyJsonToEntity(json, warband,
                    creatureNames.stream().collect(Collectors.toMap(CreatureName::getName, CreatureName::getId)));
            all.add(warband);
        }
        return Warband.persist(all);
    }

    private Uni<String> readFile(java.nio.file.Path filePath) {
        return vertx.executeBlocking(Uni.createFrom().item(() -> {
            try {
                return Files.readString(filePath);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }));
    }

    private void applyJsonToEntity(JsonObject json, Warband warband, Map<String, Long> creatureNamesToIds) {
        if (warband.id == null) {
            warband.name = json.getString("name");
        }
        warband.pointLimit = PointLimit.valueOf(json.getString("pointLimit"));
        warband.arena = json.getBoolean("arena");
        warband.note = json.getString("note");
        JsonArray creaturesJson = json.getJsonArray("creatures");

        List<Creature> creatures = warband.creatures().stream().map(WarbandCreature::getCreature).collect(Collectors.toList());

        for (Object obj : creaturesJson) {
            if (obj instanceof JsonObject) {
                JsonObject creature = (JsonObject) obj;
                String name = creature.getString("name");
                if (name == null || name.isEmpty()) {
                    LOG.warn("Unable to add a creature with no name to warband " + warband.name);
                    continue;
                }
                Creature dc = new Creature();
                dc.name = name;
                Long id = creatureNamesToIds.get(name);
                if (id == null) {
                    throw new IllegalStateException("Creature with name " + name + " does not exist");
                }
                if (warband.id == null || !creatures.remove(dc)) {
                    // Just add a new creature
                    dc.id = id;
                    warband.addCreature(dc, false);
                }
            }
        }

        if (!creatures.isEmpty()) {
            for (Creature c : creatures) {
                for (WarbandCreature wc : warband.creatures()) {
                    if (wc.creature.name.equals(c.name)) {
                        warband.removeCreature(wc.id);
                        break;
                    }
                }
            }
        }
    }

}
