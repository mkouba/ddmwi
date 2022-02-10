package com.github.mkouba.ddmwi.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.github.mkouba.ddmwi.Creature.Alignment;

public class CreatureFiltersTest {

    @Test
    public void testFilters() {
        assertFilters("Foo", " where lower(c.name) like lower(:Foo1)", Map.of("Foo1", "%Foo%"));
        assertFilters("name~Foo", " where lower(c.name) like lower(:name1)", Map.of("name1", "%Foo%"));
        assertFilters("name~\"%Foo\"", " where lower(c.name) like lower(:name1)", Map.of("name1", "%Foo"));
        assertFilters("\"Foo\"", " where lower(c.name) like lower(:name1)", Map.of("name1", "%Foo%"));
        // mine is an empty filter
        assertFilters("mine", "", Map.of());

        assertFilters("speed=10", " where c.speed = :speed1", Map.of("speed1", 10));
        assertFilters("hp>10", " where c.hp > :hp1", Map.of("hp1", 10));
        assertFilters("ac<=10", " where c.ac <= :ac1", Map.of("ac1", 10));
        assertTrue(parse("ac<foo").get().isEmpty());
        assertTrue(parse("ac_ignored").get().isEmpty());

        assertFilters("good", " where c.alignment = :good1", Map.of("good1", Alignment.GOOD));

        assertFilters("key=Unique", " where lower(c.keywords) like lower(:key1)", Map.of("key1", "%Unique%"));
        assertFilters("key=\"Unique and cool > 0\"", " where lower(c.keywords) like lower(:key1)",
                Map.of("key1", "%Unique and cool > 0%"));
        assertFilters("unique", " where c.keywords like :unique1", Map.of("unique1", "%Unique%"));

        assertFilters("wild under border civ",
                " where c.factions like '%WILD%' and c.factions like '%UNDERDARK%' and c.factions like '%BORDERLANDS%' and c.factions like '%CIVILIZATION%'",
                Map.of());

        assertFilters("key=Goblin mine", " where lower(c.keywords) like lower(:key1)", Map.of("key1", "%Goblin%"));
    }

    private void assertFilters(String query, String whereClause, Map<String, Object> params) {
        Filters filters = parse(query);
        assertEquals(whereClause, filters.getWhereClause());
        assertEquals(params, filters.getParameters());
    }

    private Filters parse(String query) {
        return CreatureDao.initFilterParser().parse(query);
    }

}
