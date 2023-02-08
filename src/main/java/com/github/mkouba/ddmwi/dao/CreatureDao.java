package com.github.mkouba.ddmwi.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.hibernate.reactive.mutiny.Mutiny.Query;
import org.jboss.logging.Logger;

import com.github.mkouba.ddmwi.Creature;
import com.github.mkouba.ddmwi.Creature.Alignment;
import com.github.mkouba.ddmwi.Creature.Faction;
import com.github.mkouba.ddmwi.CreatureView;
import com.github.mkouba.ddmwi.User;
import com.github.mkouba.ddmwi.UserCreature;
import com.github.mkouba.ddmwi.dao.FilterParser.Operator;
import com.github.mkouba.ddmwi.dao.Filters.Filter;
import com.github.mkouba.ddmwi.security.UserIdentityProvider;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class CreatureDao {

    static final Logger LOG = Logger.getLogger(CreatureDao.class);

    @Inject
    CurrentIdentityAssociation identity;

    private final FilterParser filterParser;

    private final List<Entry<String, String>> sortOptions;

    CreatureDao() {
        filterParser = initFilterParser();
        sortOptions = List.of(Map.entry("name A🠖Z", "c.name asc"), Map.entry("name Z🠖A", "c.name desc"),
                Map.entry("cost 1🠖9", "c.cost asc"), Map.entry("cost 9🠖1", "c.cost desc"), Map.entry("hp 1🠖9", "c.hp asc"),
                Map.entry("hp 9🠖1", "c.hp desc"));
    }

    public Filters parse(String queryStr) {
        return filterParser.parse(queryStr);
    }

    public List<Entry<String, String>> getSortOptions() {
        return sortOptions;
    }

    public Uni<PageResults<? extends CreatureView>> findPage(Filters filters, int pageIndex, SortInfo sortInfo,
            String whereClause,
            Map<String, Object> params) {

        // TODO add some reasonable logging about filters used
        // LOG.debugf("Find creatures: %s", filters.get());

        return identity.getDeferredIdentity().chain(i -> {

            if (filters.has("mine")) {
                // Select creatures owned by the current user
                String where = whereClause.isEmpty() ? "where u.id = :userId"
                        : whereClause + " and u.id = :userId";
                String queryStr = "select uc.id from UserCreature uc"
                        + " left join uc.creature c"
                        + " left join uc.user u "
                        + where
                        + " group by uc.id";

                params.put("userId", i.getAttribute(UserIdentityProvider.USER_ID));

                return Panache.getSession().chain(s -> {
                    // We need to find matching creatures ids first
                    Query<Long> query = s.createQuery(queryStr);
                    params.forEach(query::setParameter);

                    return query.getResultList().chain(ids -> {
                        if (ids.isEmpty()) {
                            return Uni.createFrom().item(PageResults.empty());
                        }
                        // Then if some creatures match the filtering criteria fetch the initialized entities
                        String fetchQueryStr = "select distinct uc from UserCreature uc"
                                + " left join fetch uc.creature c"
                                + " left join fetch uc.user u "
                                + " left join fetch c.powers cp"
                                + " where uc.id in (:ids) "
                                + "order by " + sortInfo.selected;

                        return UserCreature
                                .<UserCreature> find(fetchQueryStr,
                                        Map.of("ids", ids.stream().distinct().collect(Collectors.toList())))
                                // TODO this and "left join fetch c.powers" results in:
                                // HR000104: firstResult/maxResults specified with collection fetch; applying in memory!
                                .page(pageIndex, PageResults.DEFAULT_PAGE_SIZE)
                                .list().chain(results -> {
                                    if (results.isEmpty()) {
                                        return Uni.createFrom().item(PageResults.empty());
                                    }
                                    String countQueryStr = "select distinct uc.id from UserCreature uc"
                                            + " left join uc.creature c"
                                            + " left join uc.user u "
                                            + " left join c.powers cp " + where;
                                    return UserCreature.find(countQueryStr, params).count()
                                            .map(count -> new PageResults<>(results, pageIndex, count));
                                });
                    });
                });
            } else {
                String queryStr = "select c.id from Creature c "
                        + " left join c.powers cp " + whereClause
                        + " group by c.id";
                return Panache.getSession().chain(s -> {
                    // We need to find matching creatures ids first
                    Query<Long> query = s.createQuery(queryStr);
                    params.forEach(query::setParameter);
                    //query.setFirstResult(pageIndex * PageResults.DEFAULT_PAGE_SIZE);
                    //query.setMaxResults(PageResults.DEFAULT_PAGE_SIZE);

                    return query.getResultList().chain(ids -> {
                        if (ids.isEmpty()) {
                            return Uni.createFrom().item(PageResults.empty());
                        }
                        // Then if some creatures match the filtering criteria fetch the initialized entities
                        String fetchQueryStr = "select distinct c from Creature c "
                                + " left join fetch c.powers cp"
                                + " where c.id in (:ids) order by " + sortInfo.selected;
                        return Creature.<Creature> find(fetchQueryStr, Map.of("ids", ids))
                                .page(pageIndex, PageResults.DEFAULT_PAGE_SIZE)
                                .list().chain(creatures -> creatures.isEmpty() ? Uni.createFrom().item(PageResults.empty()) :

                                        UserCreature.<UserCreature> find("select uc from UserCreature uc"
                                                + " left join fetch uc.creature c"
                                                + " left join fetch uc.user u"
                                                + " where u.username = :username and c.id in (:cids)",
                                                Map.of("username", i.getPrincipal().getName(), "cids",
                                                        creatures.stream().map(Creature::getId).collect(Collectors.toList())))
                                                .list()
                                                .chain(ucr -> Creature.find("select distinct c.id from Creature c"
                                                        + " left join c.powers cp " + whereClause, params).count()
                                                        .map(count -> {
                                                            // And merge the results with user creatures
                                                            List<CreatureView> results = new ArrayList<>(creatures.size());
                                                            Map<Long, UserCreature> userCreatures = ucr.stream()
                                                                    .collect(Collectors.toMap(CreatureView::getCreatureId,
                                                                            Function.identity()));
                                                            for (Creature c : creatures) {
                                                                UserCreature uc = userCreatures.get(c.id);
                                                                results.add(uc != null ? uc : c);
                                                            }
                                                            return new PageResults<>(results, pageIndex, count);
                                                        })));
                    });
                });
            }
        });
    }

    public Uni<Void> toggleCollection(Long creatureId) {
        return identity.getDeferredIdentity()
                .chain(i -> UserCreature
                        .<UserCreature> find("from UserCreature uc where uc.user.id = :userId and uc.creature.id = :creatureId",
                                Map.of("userId", i.getAttribute(UserIdentityProvider.USER_ID), "creatureId", creatureId))
                        .firstResult().chain(uc -> {
                            if (uc == null) {
                                UserCreature userCreature = new UserCreature();
                                Creature dummyCreature = new Creature();
                                dummyCreature.id = creatureId;
                                userCreature.creature = dummyCreature;
                                User dummyUser = new User();
                                dummyUser.id = i.getAttribute(UserIdentityProvider.USER_ID);
                                userCreature.user = dummyUser;
                                return userCreature.persist().chain(e -> Uni.createFrom().voidItem());
                            } else {
                                return uc.delete();
                            }
                        }));
    }

    public Uni<List<UserCreature>> findCollection(SecurityIdentity identity) {
        return UserCreature
                .<UserCreature> find("select uc from UserCreature uc"
                        + " left join fetch uc.creature c"
                        + " left join fetch uc.user u"
                        + " where uc.user.id = :userId order by uc.creature.name asc",
                        Map.of("userId", identity.getAttribute(UserIdentityProvider.USER_ID)))
                .list();
    }

    public Uni<Creature> findCreature(long id) {
        return Creature.<Creature> find("select c from Creature c "
                + " left join fetch c.powers cp "
                + "where c.id = :id ", Map.of("id", id))
                .singleResult();
    }

    public Uni<List<CreatureName>> findAllCreatureNames() {
        return Creature.findAll().project(CreatureName.class).list();
    }

    @RegisterForReflection
    public static class CreatureName {

        private final Long id;
        private final String name;

        public CreatureName(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

    }

    static FilterParser initFilterParser() {
        Pattern word = Pattern.compile("[a-zA-Z0-9]+");
        return FilterParser.builder()
                .addDef("name").hqlName("c.name").stringOperators().toStrValue().done()
                .addDef("set").hqlName("c.setInfo").stringOperators().toStrValue().done()
                .addDef("level").hqlName("c.level").numberOperators().toIntValue().done()
                .addDef("cost").hqlName("c.cost").numberOperators().toIntValue().done()
                .addDef("hp").hqlName("c.hp").numberOperators().toIntValue().done()
                .addDef("speed").hqlName("c.speed").numberOperators().toIntValue().done()
                .addDef("mine").done()
                .addDef("keyword", "key").hqlName("c.keywords").stringOperators().syntheticOperator(Operator.LIKE)
                .toStrValue().done()
                .addDef("unique").hqlName("c.keywords").syntheticOperator(Operator.LIKE).toValue("%Unique%").done()
                .addDef("ac").hqlName("c.ac").numberOperators().toIntValue().done()
                .addDef("ref").hqlName("c.ref").numberOperators().toIntValue().done()
                .addDef("fort").hqlName("c.fort").numberOperators().toIntValue().done()
                .addDef("will").hqlName("c.will").numberOperators().toIntValue().done()
                .addDef("cr").hqlName("c.championRating").numberOperators().toIntValue().done()
                .addDef("champion").hqlName("c.championRating").syntheticOperator(Operator.GT).toValue(0).done()
                // Alignment
                .addDef("good").hqlName("c.alignment").syntheticOperator(Operator.EQ).toValue(Alignment.GOOD).done()
                .addDef("evil").hqlName("c.alignment").syntheticOperator(Operator.EQ).toValue(Alignment.EVIL).done()
                .addDef("neutral").hqlName("c.alignment").syntheticOperator(Operator.EQ).toValue(Alignment.NEUTRAL).done()
                // Factions
                .addDef("wild").hqlName("c.factions").syntheticOperator(Operator.LIKE).toNullValue()
                .toString(factionToString(Faction.WILD)).done()
                .addDef("civilization", "civ").hqlName("c.factions").syntheticOperator(Operator.LIKE).toNullValue()
                .toString(factionToString(Faction.CIVILIZATION)).done()
                .addDef("borderlands", "border").hqlName("c.factions").syntheticOperator(Operator.LIKE).toNullValue()
                .toString(factionToString(Faction.BORDERLANDS)).done()
                .addDef("underdark", "under").hqlName("c.factions").syntheticOperator(Operator.LIKE).toNullValue()
                .toString(factionToString(Faction.UNDERDARK)).done()
                // Match any word as a name
                .addDef("word").nameMatcher(v -> word.matcher(v).matches()).hqlName("c.name").syntheticOperator(Operator.LIKE)
                .toString(
                        f ->
                        // lower(name) like lower(:name)
                        new StringBuilder()
                                .append("lower(")
                                .append(f.hqlName)
                                .append(") like lower(:")
                                .append(f.getParamName())
                                .append(")").toString())
                .toStrValue().done()
                .adaptQueryPart(p -> {
                    if (p.startsWith("\"") && p.endsWith("\"")) {
                        // string literal with no operator -> name like
                        return "name~" + p;
                    }
                    return p;
                })
                .build();
    }

    static Function<Filter, String> factionToString(Faction faction) {
        // Note that we can't use query params here due to our factions converter
        return f -> new StringBuilder().append("c.factions like " + toLikeContains(faction)).toString();
    }

    static String toLikeContains(Object val) {
        return "'%" + val.toString() + "%'";
    }

}
