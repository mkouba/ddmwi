package com.github.mkouba.ddmwi.ctrl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.RestResponse.ResponseBuilder;

import com.github.mkouba.ddmwi.Creature;
import com.github.mkouba.ddmwi.CreaturePower;
import com.github.mkouba.ddmwi.dao.CreatureDao;
import com.github.mkouba.ddmwi.dao.Filters;
import com.github.mkouba.ddmwi.dao.SortInfo;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/creature-export")
public class CreatureExport extends Controller {

    private static Logger LOG = Logger.getLogger(CreaturesImport.class);

    @Inject
    CreatureDao creatureDao;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<RestResponse<String>> get(@RestQuery String q, @RestQuery String sortBy) {
        SortInfo sortInfo = new SortInfo(sortBy, creatureDao.getSortOptions());
        Filters filters = creatureDao.parse(q);
        String queryStr = "select distinct c from Creature c left join fetch c.powers cp " + filters.getWhereClause()
                + " order by "
                + sortInfo.selected;
        return Creature.<Creature> find(queryStr, filters.getParameters()).list().chain(this::creaturesToJson)
                .map(buffer -> ResponseBuilder.ok(buffer).header("Content-Disposition",
                        "attachment; filename=creatures-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".json")
                        .build());
    }

    private Uni<String> creaturesToJson(List<Creature> creatures) {
        LOG.infof("Export %s creatures", creatures.size());
        JsonArray all = new JsonArray();
        for (Creature creature : creatures) {
            all.add(creatureToJson(creature));
        }
        return Uni.createFrom().item(all.encodePrettily());
    }

    static JsonObject creatureToJson(Creature creature) {
        JsonObject json = new JsonObject();
        json.put("name", creature.name);
        json.put("alignment", creature.alignment);
        json.put("movementMode", creature.movementMode);
        json.put("cost", creature.cost);
        json.put("level", creature.level);
        json.put("speed", creature.speed);
        json.put("hp", creature.hp);
        json.put("ac", creature.ac);
        json.put("fort", creature.ref);
        json.put("ref", creature.ref);
        json.put("will", creature.ref);

        JsonArray factions = new JsonArray();
        creature.factions.forEach(factions::add);
        json.put("factions", factions);

        JsonArray keywords = new JsonArray();
        creature.getKeywordsList().forEach(keywords::add);
        if (!keywords.isEmpty()) {
            json.put("keywords", keywords);
        }

        if (creature.championRating > 0) {
            json.put("championRating", creature.championRating);
        }

        creature.powers.sort(Comparator.comparing(CreaturePower::getType).thenComparing(CreaturePower::getText));
        JsonArray powers = new JsonArray();
        for (CreaturePower p : creature.powers) {
            JsonObject power = new JsonObject();
            power.put("type", p.type.toString());
            power.put("text", p.text);
            power.put("limit", p.usageLimit);
            powers.add(power);
        }
        json.put("powers", powers);

        return json;
    }

}
