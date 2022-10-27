package com.github.mkouba.ddmwi.ctrl;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
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

import com.github.mkouba.ddmwi.Creature;
import com.github.mkouba.ddmwi.Creature.Alignment;
import com.github.mkouba.ddmwi.Creature.Faction;
import com.github.mkouba.ddmwi.Creature.MovementMode;
import com.github.mkouba.ddmwi.CreaturePower;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;

@Path("/creatures/import")
public class CreaturesImport extends Controller {

    private static Logger LOG = Logger.getLogger(CreaturesImport.class);

    @Inject
    Vertx vertx;

    @NonBlocking
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get() {
        return Templates.creaturesImport();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_HTML)
    public Uni<RestResponse<Object>> importCreatures(@MultipartForm FileImportForm form) {
        URI listUri = uriInfo.getRequestUriBuilder().replacePath("/creature-list").build();
        return importCreaturesJson(form).map(v -> RestResponse.seeOther(listUri));
    }

    Uni<Void> importCreaturesJson(FileImportForm form) {
        java.nio.file.Path filePath = form.importFile.filePath();
        LOG.infof("Import creatures from file: %s", filePath);
        return readFile(filePath).chain(str -> Panache.withTransaction(
                () -> Creature.<Creature> find(
                        "select distinct c from Creature c left join fetch c.powers cp")
                        .list().chain(creatures -> updateCreatures(str, creatures))));
    }

    private Uni<Void> updateCreatures(String str, List<Creature> creatures) {
        if (str == null || str.isEmpty()) {
            return Uni.createFrom().voidItem();
        }
        JsonArray jsonData = new JsonArray(str);
        List<Creature> all = new ArrayList<>();
        for (Object o : jsonData) {
            JsonObject json = (JsonObject) o;
            String name = json.getString("name");
            if (name == null) {
                // Creature name is mandatory
                continue;
            }
            Optional<Creature> existing = creatures.stream().filter(c -> c.name.equals(name))
                    .findAny();
            Creature creature = existing.orElse(new Creature());
            applyJsonToEntity(json, creature);
            all.add(creature);
        }
        return Creature.persist(all);
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

    private void applyJsonToEntity(JsonObject json, Creature creature) {
        if (creature.id == null) {
            creature.name = json.getString("name");
        }
        setIntVal(json, "cost", val -> creature.cost = val);
        setIntVal(json, "level", val -> creature.level = val);
        setIntVal(json, "ac", val -> creature.ac = val);
        setIntVal(json, "will", val -> creature.will = val);
        setIntVal(json, "ref", val -> creature.ref = val);
        setIntVal(json, "fort", val -> creature.fort = val);
        setIntVal(json, "hp", val -> creature.hp = val);
        setIntVal(json, "speed", val -> creature.speed = val);

        if (json.containsKey("championRating")) {
            creature.championRating = json.getInteger("championRating");
        }
        if (json.containsKey("alignment")) {
            creature.alignment = Alignment.valueOf(json.getString("alignment"));
        }
        if (json.containsKey("movementMode")) {
            creature.movementMode = MovementMode.valueOf(json.getString("movementMode"));
        }
        JsonArray factions = json.getJsonArray("factions");
        if (factions == null || factions.isEmpty()) {
            Faction defaultFaction = Faction.BORDERLANDS;
            LOG.warnf(
                    "No faction specified for creature %s - at least one faction must be used; %s was set as the default faction",
                    creature, defaultFaction);
            creature.factions = Collections.singleton(defaultFaction);
        } else {
            creature.factions = factions.stream().map(Object::toString).map(Faction::valueOf)
                    .collect(Collectors.toSet());
        }
        JsonArray keywords = json.getJsonArray("keywords");
        if (keywords != null && !keywords.isEmpty()) {
            creature.keywords = keywords.stream().map(Object::toString)
                    .collect(Collectors.joining(", "));
        }
        JsonArray powers = json.getJsonArray("powers");
        creature.powers.clear();
        
        if (powers != null && !powers.isEmpty()) {
            for (Object p : powers) {
                JsonObject powerJson = (JsonObject) p;
                CreaturePower power = new CreaturePower();
                power.type = CreaturePower.PowerType.valueOf(powerJson.getString("type"));
                power.text = powerJson.getString("text");
                power.usageLimit = powerJson.getInteger("limit");
                creature.addPower(power);
            }
        }

        creature.setInfo = json.getString("setInfo");
    }

    private void setIntVal(JsonObject json, String key, Consumer<Integer> update) {
        Integer val = json.getInteger(key);
        if (val != null) {
            update.accept(val);
        }
    }

}
