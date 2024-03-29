package com.github.mkouba.ddmwi.ctrl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import org.jboss.resteasy.reactive.RestQuery;

import com.github.mkouba.ddmwi.Warband;
import com.github.mkouba.ddmwi.dao.Filters;
import com.github.mkouba.ddmwi.dao.PageResults;
import com.github.mkouba.ddmwi.dao.SortInfo;
import com.github.mkouba.ddmwi.dao.WarbandDao;

import io.quarkus.qute.TemplateExtension;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path(WarbandList.PATH)
public class WarbandList extends Controller {

    static final String PATH = "/warband-list";

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
    public Uni<TemplateInstance> list(@RestQuery String q, @RestQuery int page, @RestQuery String sortBy) {
        SortInfo sortInfo = new SortInfo(sortBy, warbandDao.getSortOptions());
        Filters filters = warbandDao.parse(q);
        Uni<PageResults<Warband>> pageResults = warbandDao.findPage(page < 1 ? 0 : page - 1, sortInfo,
                filters.getWhereClause(),
                filters.getParameters());
        return pageResults.map(pr -> Templates.warbands(pr, q, sortInfo));
    }

    @GET
    @Path("page")
    @Produces(MediaType.TEXT_HTML)
    public Uni<TemplateInstance> page(@RestQuery String q, @RestQuery int page, @RestQuery String sortBy) {
        SortInfo sortInfo = new SortInfo(sortBy, warbandDao.getSortOptions());
        Filters filters = warbandDao.parse(q);
        Uni<PageResults<Warband>> pageResults = warbandDao.findPage(page < 1 ? 0 : page - 1, sortInfo,
                filters.getWhereClause(),
                filters.getParameters());
        setHtmxPush("/warband-list?q=%s&sortBy=%s&page=%s", q, sortBy, page);
        return pageResults.map(pr -> Tags.warbandCards(pr, q, sortInfo, "/warband-list/page", "#warbands"));
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
