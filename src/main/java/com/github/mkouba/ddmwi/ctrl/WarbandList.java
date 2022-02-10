package com.github.mkouba.ddmwi.ctrl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestQuery;

import com.github.mkouba.ddmwi.Warband;
import com.github.mkouba.ddmwi.dao.Filters;
import com.github.mkouba.ddmwi.dao.PageResults;
import com.github.mkouba.ddmwi.dao.SortInfo;
import com.github.mkouba.ddmwi.dao.WarbandDao;

import io.quarkus.qute.TemplateExtension;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerResponse;

@Path("/warband-list")
public class WarbandList extends Controller {

    static final List<Map.Entry<String, String>> QUICK_FILTERS = List.of(
            Map.entry("Arena warbands", "arena"),
            Map.entry("Standard point limit", "standard"),
            Map.entry("Epic point limit", "epic"),
            Map.entry("Quick point limit", "quick"),
            Map.entry("With champion", "champion"),
            Map.entry("Good", "good"),
            Map.entry("Evil", "evil"),
            Map.entry("Warband name contains \"dwarf\"", "&quot;dwarf&quot;"));

    @Inject
    WarbandDao warbandDao;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list(@RestQuery String q, @RestQuery int page, @RestQuery String sortBy) {
        SortInfo sortInfo = new SortInfo(sortBy, warbandDao.getSortOptions());
        Filters filters = warbandDao.parse(q);
        Uni<PageResults<Warband>> pageResults = warbandDao.findPage(page < 1 ? 0 : page - 1, sortInfo,
                filters.getWhereClause(),
                filters.getParameters());
        return Templates.warbands(pageResults.memoize().indefinitely(), q, sortInfo);
    }

    @GET
    @Path("page")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance page(@RestQuery String q, @RestQuery int page, @RestQuery String sortBy,
            HttpServerResponse response) {
        SortInfo sortInfo = new SortInfo(sortBy, warbandDao.getSortOptions());
        Filters filters = warbandDao.parse(q);
        Uni<PageResults<Warband>> pageResults = warbandDao.findPage(page < 1 ? 0 : page - 1, sortInfo,
                filters.getWhereClause(),
                filters.getParameters());
        setHxPushHeader(response, "/warband-list?q=%s&sortBy=%s&page=%s", q, sortBy, page);
        return Tags.warbandCards(pageResults.memoize().indefinitely(), q, sortInfo, "/warband-list/page", "#warbands");
    }

    @TemplateExtension(namespace = "warband")
    static List<Map.Entry<String, String>> quickFilters() {
        return QUICK_FILTERS;
    }

    // TODO this is not very nice
    @TemplateExtension(namespace = "warband")
    public static String availableCreaturesPath(Long warbandId) {
        return "/warband-detail/" + warbandId + "/available-creatures";
    }

    @TemplateExtension(namespace = "warband", priority = 6)
    public static int noteLimit() {
        return Warband.NOTE_LIMIT;
    }

    @TemplateExtension
    static int pointsLimitPercentage(Warband warband) {
        BigDecimal limit = new BigDecimal(warband.pointLimit.value);
        BigDecimal percent = limit.divide(new BigDecimal(100));
        BigDecimal points = new BigDecimal(warband.getTotalCost());
        return points.divide(percent).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    @TemplateExtension
    static int pointsRemainingPercentage(Warband warband) {
        return new BigDecimal(100).subtract(new BigDecimal(pointsLimitPercentage(warband))).setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }

    @TemplateExtension
    static int creaturesLimitPercentage(Warband warband) {
        BigDecimal limit = new BigDecimal(warband.getCreaturesLimit());
        BigDecimal percent = limit.divide(new BigDecimal(100));
        BigDecimal points = new BigDecimal(warband.creatures.size());
        return points.divide(percent).setScale(0, RoundingMode.HALF_UP).intValue();
    }

}
