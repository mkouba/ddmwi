package com.github.mkouba.ddmwi.dao;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.Function;

public class Filters {

    public static final Filters EMPTY = new Filters(Collections.emptyList());

    private final List<Filter> filters;
    
    Filters(List<Filter> filters) {
        this.filters = filters;
    }

    public String getWhereClause() {
        if (filters.isEmpty()) {
            return "";
        }
        StringBuilder clause = new StringBuilder();
        for (ListIterator<Filter> it = filters.listIterator(); it.hasNext();) {
            Filter filter = it.next();
            if (filter.isEmpty()) {
                continue;
            }
            if (clause.length() == 0) {
                clause.append(" where");
            } else {
                clause.append(" and");
            }
            clause.append(" ");
            clause.append(filter);
        }
        return clause.toString();
    }

    public List<Filter> get() {
        return filters;
    }

    public boolean has(String name) {
        return get(name) != null;
    }

    public Filter get(String name) {
        for (Filter filter : filters) {
            if (filter.name.equals(name)) {
                return filter;
            }
        }
        return null;
    }

    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<>();
        for (Filter filter : filters) {
            if (!filter.isEmpty() && filter.value != null) {
                params.put(filter.getParamName(), filter.value);
            }
        }
        return params;
    }
    
    public boolean isEmpty() {
        return filters.isEmpty() || filters.stream().allMatch(Filter::isEmpty);
    }

    public static class Filter {

        public final int idx;
        public final String name;
        public final String hqlName;
        public final String operator;
        public final Object value;
        public final Function<Filter, String> toString;

        Filter(int idx, String name, String hqlName, String operator, Object value, Function<Filter, String> toString) {
            this.idx = idx;
            this.name = name;
            this.hqlName = hqlName;
            this.operator = operator;
            this.value = value;
            this.toString = toString;
        }

        String getParamName() {
            return name + idx;
        }

        String toDefaultString() {
            return new StringBuilder()
                    // name = :name
                    .append(hqlName)
                    .append(" ")
                    .append(operator)
                    .append(" :")
                    .append(getParamName())
                    .toString();
        }

        boolean isEmpty() {
            return hqlName == null;
        }

        @Override
        public String toString() {
            if (toString != null) {
                return toString.apply(this);
            }
            return toDefaultString();
        }

    }

}