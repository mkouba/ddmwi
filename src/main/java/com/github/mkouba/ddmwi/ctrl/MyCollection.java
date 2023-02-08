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
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.RestResponse.ResponseBuilder;

import com.github.mkouba.ddmwi.Creature;
import com.github.mkouba.ddmwi.User;
import com.github.mkouba.ddmwi.UserCreature;
import com.github.mkouba.ddmwi.dao.CreatureDao;
import com.github.mkouba.ddmwi.dao.CreatureDao.CreatureName;
import com.github.mkouba.ddmwi.security.UserIdentityProvider;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/collection")
public class MyCollection extends Controller {

    private static Logger LOG = Logger.getLogger(MyCollection.class);

    @Inject
    CurrentIdentityAssociation identity;

    @Inject
    CreatureDao creatureDao;

    @Inject
    Vertx vertx;

    @GET
    @Path("/export")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<RestResponse<String>> exportCollection() {
        return identity.getDeferredIdentity().chain(i -> creatureDao.findCollection(i).chain(c -> creaturesToJson(i, c)));
    }

    @GET
    @Path("/import")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get() {
        return Templates.collectionImport();
    }

    @POST
    @Path("/import")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_HTML)
    public Uni<RestResponse<Object>> importCollection(@MultipartForm FileImportForm form) {
        URI listUri = uriFrom(CreatureList.PATH);
        return importJson(form).map(v -> RestResponse.seeOther(listUri));
    }

    Uni<Void> importJson(FileImportForm form) {
        java.nio.file.Path filePath = form.importFile.filePath();
        return readFile(filePath).chain(
                str -> creatureDao.findAllCreatureNames().chain(
                        cn -> identity.getDeferredIdentity().chain(
                                i -> creatureDao.findCollection(i).chain(
                                        c -> Panache.withTransaction(() -> updateCollection(filePath, i, str, c, cn))))));
    }

    private Uni<Void> updateCollection(java.nio.file.Path filePath, SecurityIdentity identity, String str,
            List<UserCreature> collection, List<CreatureName> creatureNames) {
        JsonObject jsonData = new JsonObject(str);
        JsonArray collectionJson = jsonData.getJsonArray("creatures");

        LOG.infof("Importing collection of %s creatures for user [%s]", collectionJson.size(),
                identity.getPrincipal().getName());

        Map<Creature, Long> collectionMap = collection.stream()
                .collect(Collectors.toMap(UserCreature::creature, UserCreature::getId));
        Map<String, Long> creatureNamesToIds = creatureNames.stream()
                .collect(Collectors.toMap(CreatureName::getName, CreatureName::getId));
        List<UserCreature> newCreatures = new ArrayList<>();

        for (Object o : collectionJson) {
            JsonObject json = (JsonObject) o;
            String name = json.getString("name");
            if (name == null || name.isEmpty()) {
                continue;
            }
            Long id = creatureNamesToIds.get(name);
            if (id == null) {
                LOG.warnf("Creature with name [%s] does not exist [importFile: %s]", name, filePath);
                continue;
            }
            Creature dc = Creature.createDummy(id);
            dc.name = name;
            if (collectionMap.remove(dc) == null) {
                // Add a new creature to collection
                UserCreature uc = new UserCreature();
                uc.creature = dc;
                User du = new User();
                du.id = identity.getAttribute(UserIdentityProvider.USER_ID);
                uc.user = du;
                newCreatures.add(uc);
            }
        }

        if (!collectionMap.isEmpty()) {
            return UserCreature.delete("delete from UserCreature where id in (:ids)",
                    Map.of("ids", collectionMap.values()))
                    .chain(c -> UserCreature.persist(newCreatures));
        } else {
            return UserCreature.persist(newCreatures);
        }
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

    private Uni<RestResponse<String>> creaturesToJson(SecurityIdentity identity, List<UserCreature> creatures) {
        String filename = "collection-" + identity.getPrincipal().getName() + "-"
                + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".json";
        JsonObject export = new JsonObject();
        export.put("user", identity.getPrincipal().getName());
        export.put("created", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        JsonArray all = new JsonArray();
        export.put("creatures", all);
        for (UserCreature creature : creatures) {
            all.add(new JsonObject().put("name", creature.creature.name));
        }
        LOG.infof("Exported collection of user [%s] - creatures found: %s", identity.getPrincipal().getName(),
                creatures.size());
        return Uni.createFrom().item(ResponseBuilder.ok(export.encodePrettily()).header("Content-Disposition",
                "attachment; filename=" + filename)
                .build());
    }

}
