package com.github.mkouba.ddmwi.dao;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.mutiny.Mutiny.Query;
import org.jboss.logging.Logger;

import com.github.mkouba.ddmwi.CreaturePower;
import com.github.mkouba.ddmwi.CreatureView;
import com.github.mkouba.ddmwi.Warband;
import com.github.mkouba.ddmwi.WarbandCreature;
import com.github.mkouba.ddmwi.Creature.Alignment;
import com.github.mkouba.ddmwi.Creature.Faction;
import com.github.mkouba.ddmwi.Warband.PointLimit;
import com.github.mkouba.ddmwi.dao.FilterParser.Operator;
import com.github.mkouba.ddmwi.security.UserIdentityProvider;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;

@Singleton
public class WarbandDao {

    static final Logger LOG = Logger.getLogger(WarbandDao.class);

    @Inject
    CurrentIdentityAssociation identity;

    @Inject
    CreatureDao creatureDao;

    private final FilterParser filterParser;
    private final List<Entry<String, String>> sortOptions;

    WarbandDao() {
        filterParser = initFilterParser();
        sortOptions = List.of(Map.entry("name A🠖Z", "w.name asc"), Map.entry("name Z🠖A", "w.name desc"));
    }

    static FilterParser initFilterParser() {
        return FilterParser.builder()
                .addDef("name").hqlName("w.name").stringOperators().toStrValue().done()
                .addDef("note").hqlName("w.note").stringOperators().toStrValue().done()
                .addDef("arena").hqlName("w.arena").syntheticOperator(Operator.EQ).toValue(true).done()
                .addDef("standard", PointLimit.STANDARD.value + "").hqlName("w.pointLimit").syntheticOperator(Operator.EQ)
                .toValue(PointLimit.STANDARD).done()
                .addDef("epic", PointLimit.EPIC.value + "").hqlName("w.pointLimit").syntheticOperator(Operator.EQ)
                .toValue(PointLimit.EPIC).done()
                .addDef("quick", PointLimit.QUICK.value + "").hqlName("w.pointLimit").syntheticOperator(Operator.EQ)
                .toValue(PointLimit.QUICK).done()
                .addDef("champ", "champion", "withChampion").hqlName("c.championRating").syntheticOperator(Operator.GT)
                .toValue(0).done()
                .addDef("good").hqlName("c.alignment").syntheticOperator(Operator.EQ).toValue(Alignment.GOOD).done()
                .addDef("evil").hqlName("c.alignment").syntheticOperator(Operator.EQ).toValue(Alignment.EVIL).done()
                .adaptQueryPart(p -> {
                    if (p.startsWith("\"") && p.endsWith("\"")) {
                        // string literal with no operator -> name like
                        return "name~" + p;
                    }
                    return p;
                })
                .build();
    }

    public Filters parse(String queryStr) {
        return filterParser.parse(queryStr);
    }

    public List<Entry<String, String>> getSortOptions() {
        return sortOptions;
    }

    public Uni<PageResults<Warband>> findPage(int pageIndex, SortInfo sortInfo,
            String whereClause, Map<String, Object> params) {
        // Note that we need to find matching warbands ids first in order to avoid inefficient queries and the following hibernate warning:
        // HR000104: firstResult/maxResults specified with collection fetch
        // https://stackoverflow.com/questions/11431670/how-can-i-avoid-the-warning-firstresult-maxresults-specified-with-collection-fe

        return identity.getDeferredIdentity().chain(i -> {
            String where = whereClause.isEmpty() ? "where u.username = :username" : whereClause + " and u.username = :username";
            String queryStr = "select w.id from Warband w"
                    + " left join w.creatures cr"
                    + " left join cr.creature c"
                    + " left join w.user u " + where
                    + " group by w.id"
                    + " order by " + sortInfo.selected;
            String fetchQueryStr = "select distinct w from Warband w "
                    + " left join fetch w.creatures cr "
                    + " left join fetch cr.creature c"
                    + " where w.id in (:ids)"
                    + " order by " + sortInfo.selected;
            String countQueryStr = "select distinct w from Warband w"
                    + " left join w.creatures cr"
                    + " left join cr.creature c"
                    + " left join w.user u " + where;
            params.put("username", i.getPrincipal().getName());

            LOG.debugf("Find warbands:\n\t- %s\n\t- %s", queryStr, params);

            return Panache.getSession().chain(s -> {
                Query<Long> query = s.createQuery(queryStr);
                params.forEach(query::setParameter);
                query.setFirstResult(pageIndex * PageResults.DEFAULT_PAGE_SIZE);
                query.setMaxResults(PageResults.DEFAULT_PAGE_SIZE);

                return query.getResultList().chain(ids -> {
                    if (ids.isEmpty()) {
                        return Uni.createFrom().item(PageResults.empty());
                    }
                    // Then if some warbands match the filtering criteria fetch the initialized entities
                    return Warband.<Warband> find(fetchQueryStr, Map.of("ids", ids))
                            .list().chain(
                                    results -> Warband
                                            .find(countQueryStr, params)
                                            .count().map(count -> new PageResults<>(results, pageIndex, count)));
                });
            });
        });
    }

    public Uni<Warband> findWarband(Long id) {
        return identity.getDeferredIdentity().chain(i -> {
            return Warband.<Warband> find(
                    "select w from Warband w "
                            + " left join fetch w.creatures cr"
                            + " left join fetch cr.creature c"
                            + " left join fetch w.user u"
                            + " where w.id = :id and u.id = :userId ",
                    Map.of("id", id, "userId", i.getAttribute(UserIdentityProvider.USER_ID)))
                    .singleResult()
                    // Fetch the powers of the creatures
                    // Note that if we tried to fetch the powers in the query above we would get "cannot simultaneously fetch multiple bags" 
                    .call(w -> {
                        if (w.creatures == null || w.creatures.isEmpty()) {
                            return Uni.createFrom().voidItem();
                        }
                        // IMPL. NOTE: never use Uni.join() together with hibernate reactive
                        Uni<List<CreaturePower>> uni = Uni.createFrom().nullItem();
                        for (WarbandCreature warbandCreature : w.creatures) {
                            uni = uni.chain(ignored -> Mutiny.fetch(warbandCreature.creature.powers));
                        }
                        return uni;
                    });
        });
    }

    public Uni<Warband> findWarbandLink(Long id) {
        return Warband.<Warband> find(
                "select w from Warband w "
                        + " left join fetch w.creatures cr"
                        + " left join fetch cr.creature c"
                        + " where w.id = :id and w.publicLink = true ",
                Map.of("id", id))
                .singleResult()
                // Fetch the powers of the creatures
                // Note that if we tried to fetch the powers in the query above we would get "cannot simultaneously fetch multiple bags" 
                .call(w -> {
                    if (w.creatures == null || w.creatures.isEmpty()) {
                        return Uni.createFrom().voidItem();
                    }
                    // IMPL. NOTE: never use Uni.join() together with hibernate reactive
                    Uni<List<CreaturePower>> uni = Uni.createFrom().nullItem();
                    for (WarbandCreature warbandCreature : w.creatures) {
                        uni = uni.chain(ignored -> Mutiny.fetch(warbandCreature.creature.powers));
                    }
                    return uni;
                });
    }

    public Uni<PageResults<? extends CreatureView>> findWarbandCreatures(Uni<Warband> warband, Filters filters, int page,
            SortInfo sortInfo) {
        return warband.chain(w -> findWarbandCreatures(w, filters, page, sortInfo));
    }

    public Uni<PageResults<? extends CreatureView>> findWarbandCreatures(Warband warband, Filters filters, int page,
            SortInfo sortInfo) {
        Alignment alignment = warband.getAlignment();

        Map<String, Object> params = filters.getParameters();
        StringBuilder whereStr = new StringBuilder();

        whereStr.append(filters.getWhereClause());

        if (!warband.freestyle) {
            // remaining points
            if (filters.isEmpty()) {
                whereStr.append(" where ");
            } else {
                whereStr.append(" and ");
            }
            whereStr.append("c.cost <= :remainingPoints ");
            params.put("remainingPoints", warband.getRemainingPoints());

            // alignment
            if (alignment != Alignment.NEUTRAL) {
                params.put("neutralAlignment", Alignment.NEUTRAL);
                params.put("alignment", warband.getAlignment());
                whereStr.append("and (c.alignment = :neutralAlignment or c.alignment = :alignment) ");
            }

            // unique must not be selected
            List<Long> uniqueIds = warband.creatures.stream().filter(CreatureView::isUnique).map(CreatureView::getCreatureId)
                    .collect(Collectors.toList());
            if (!uniqueIds.isEmpty()) {
                params.put("ids", uniqueIds);
                whereStr.append("and c.id not in (:ids) ");
            }

            // base factions
            Set<Faction> baseFactions = warband.getBaseFactions();
            if (!baseFactions.isEmpty() && baseFactions.size() != Faction.values().length) {
                whereStr.append("and (");
                for (Iterator<Faction> it = baseFactions.iterator(); it.hasNext();) {
                    whereStr.append("c.factions like " + CreatureDao.toLikeContains(it.next()));
                    if (it.hasNext()) {
                        whereStr.append(" or ");
                    }
                }
                whereStr.append(") ");
            }
        }

        int pageIndex = page < 1 ? 0 : page - 1;
        return creatureDao.findPage(filters, pageIndex, sortInfo, whereStr.toString(), params);
    }

    public Uni<List<Warband>> findAllWarbands() {
        return Warband.find(
                "select distinct w from Warband w"
                        + " join fetch w.creatures cr "
                        + " join fetch cr.creature c")
                .list();
    }

    public Uni<List<Warband>> findAllWarbands(SecurityIdentity identity) {
        return Warband
                .<Warband> find("select distinct w from Warband w"
                        + " left join fetch w.user u"
                        + " left join fetch w.creatures cr"
                        + " left join fetch cr.creature c"
                        + " where u.id = :userId order by w.name asc",
                        Map.of("userId", identity.getAttribute(UserIdentityProvider.USER_ID)))
                .list();
    }

    public Uni<List<Warband>> findRelevantWarbands(long creatureId) {
        return Warband.find(
                "select distinct w from Warband w"
                        + " join fetch w.creatures cr "
                        + " join fetch cr.creature c"
                        + " where c.id = :creatureId",
                Map.of("creatureId", creatureId))
                .list();
    }

}
