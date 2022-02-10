package com.github.mkouba.ddmwi.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.mkouba.ddmwi.dao.Filters.Filter;

public class FilterParserTest {

    @Test
    public void testStrLiteral() {
        FilterParser parser = FilterParser.builder()
                .addDef("name").stringOperators().toStrValue().done()
                .build();
        List<Filter> filters = parser.parse("name=\"Name and surname\"").get();
        assertEquals(1, filters.size());
        assertEquals("name", filters.get(0).name);
        assertEquals("=", filters.get(0).operator);
        assertEquals("Name and surname", filters.get(0).value);
    }

    @Test
    public void testNumberOperators() {
        FilterParser parser = FilterParser.builder()
                .addDef("cost").numberOperators().toIntValue().done()
                .build();
        List<Filter> filters = parser.parse("cost=10 cost>10 cost>=10 cost<10 cost<=10").get();
        assertEquals(5, filters.size());
        assertEquals("cost", filters.get(0).name);
        assertEquals("=", filters.get(0).operator);
        assertEquals(10, filters.get(0).value);
        assertEquals(">", filters.get(1).operator);
        assertEquals(">=", filters.get(2).operator);
        assertEquals("<", filters.get(3).operator);
        assertEquals("<=", filters.get(4).operator);
    }

}
