package com.github.mkouba.ddmwi.ctrl;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.RestResponse;

import com.github.mkouba.ddmwi.CreaturePower;
import com.github.mkouba.ddmwi.CreatureView;
import com.github.mkouba.ddmwi.dao.CreatureDao;
import com.github.mkouba.ddmwi.dao.Filters;
import com.github.mkouba.ddmwi.dao.PageResults;
import com.github.mkouba.ddmwi.dao.SortInfo;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.qute.TemplateExtension;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.mutiny.Uni;

@Path(CreatureList.PATH)
public class CreatureList extends Controller {

    static final String PATH = "/creature-list";

    static final Logger LOG = Logger.getLogger(CreatureList.class);

    static final List<Map.Entry<String, String>> QUICK_FILTERS = List.of(
            Map.entry("Your collection", "mine"),
            Map.entry("Unique creatures", "unique"),
            Map.entry("Good", "good"),
            Map.entry("Evil", "evil"),
            Map.entry("Champions", "champion"),
            Map.entry("Borderlands", "border"),
            Map.entry("Civilization", "civ"),
            Map.entry("Wild", "wild"),
            Map.entry("Underdark", "under"),
            Map.entry("Released in the set: Archfiends", "set=&quot;Archfiends&quot;"),
            Map.entry("Its name contains \"human\"", "&quot;human&quot;"),
            Map.entry("Keywords contain \"goblin\"", "key=goblin"),
            Map.entry("Has more than 10 hit points", "hp>10"),
            Map.entry("Has speed equal to 8", "speed=8"));

    @Inject
    CreatureDao creatureDao;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get(@RestQuery String q, @RestQuery int page, @RestQuery String sortBy) {
        SortInfo sortInfo = new SortInfo(sortBy, creatureDao.getSortOptions());
        Filters filters = creatureDao.parse(q);
        Uni<PageResults<? extends CreatureView>> pageResults = creatureDao.findPage(filters, page < 1 ? 0 : page - 1, sortInfo,
                filters.getWhereClause(),
                filters.getParameters());
        return Templates.creatures(pageResults.memoize().indefinitely(), q, sortInfo);
    }

    @GET
    @Path("page")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance page(@RestQuery String q, @RestQuery int page, @RestQuery String sortBy) {
        SortInfo sortInfo = new SortInfo(sortBy, creatureDao.getSortOptions());
        Filters filters = creatureDao.parse(q);
        Uni<PageResults<? extends CreatureView>> pageResults = creatureDao.findPage(filters, page < 1 ? 0 : page - 1, sortInfo,
                filters.getWhereClause(),
                filters.getParameters());
        setHtmxPush("/creature-list/?q=%s&sortBy=%s&page=%s", q, sortBy, page);
        return Tags.creatureCards(null,
                pageResults.memoize().indefinitely(),
                q, sortInfo, "/creature-list/page", "#creatures");
    }

    @Path("toggle-collection/{id}")
    @POST
    public Uni<RestResponse<Object>> toggleCollection(Long id) {
        return Panache.withTransaction(
                () -> creatureDao.toggleCollection(id).map(v -> RestResponse.ok()));

    }

    @TemplateExtension(namespace = "creature")
    static List<Map.Entry<String, String>> quickFilters() {
        return QUICK_FILTERS;
    }

    @TemplateExtension
    static String searchQuery(CreatureView creature) {
        return encode(creature.getName() + " " + (creature.getSetInfo() != null ? creature.getSetInfo() : ""));
    }

    @TemplateExtension
    static String filteredText(CreaturePower power) {
        return power.text
                // base attack
                .replace("{b}",
                        "<span class=\"fa-stack\" title=\"Basic attack\"><i class=\"far fa-circle fa-stack-2x\"></i><i class=\"fas fa-bolt fa-stack-1x\"></i></span>")
                // melee
                .replace("{m}", "&nbsp;<i class=\"fas fa-bolt fa-lg\" title=\"Melee attack\"></i>")
                // ranged
                .replace("{r}", "&nbsp;<i class=\"fas fa-crosshairs fa-lg\" title=\"Ranged attack\"></i>");
    }

}
