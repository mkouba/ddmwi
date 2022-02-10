package com.github.mkouba.ddmwi.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import com.github.mkouba.ddmwi.dao.Filters.Filter;

import io.quarkus.qute.Qute;

public class FilterParser {

    private static final Logger LOG = Logger.getLogger(FilterParser.class);

    static Pattern stringLiteral = Pattern.compile("\".*?\"");

    private final List<FilterParser.Definition> defs;

    private final Function<String, String> queryPartAdapter;

    private FilterParser(List<FilterParser.Definition> defs, Function<String, String> queryPartAdapter) {
        this.defs = defs;
        this.queryPartAdapter = queryPartAdapter;
    }

    /**
     * name=Lu cost>10 level=1
     * 
     * @param query
     * @return the filters
     */
    public Filters parse(String query) {
        if (query == null) {
            return Filters.EMPTY;
        }

        List<Filters.Filter> filters = new ArrayList<>();
        Map<String, String> literals;

        // First replace string literals
        if (query.contains("\"")) {
            literals = new HashMap<>();
            Matcher m = stringLiteral.matcher(query);
            StringBuilder builder = new StringBuilder();
            int idx = 0;
            while (m.find()) {
                String key = "__literal" + idx;
                literals.put(key, m.group());
                m.appendReplacement(builder, key);
                idx++;
            }
            m.appendTail(builder);
            query = builder.toString();
        } else {
            literals = Collections.emptyMap();
        }

        String[] parts = query.trim().split("\\s++");
        AtomicInteger idx = new AtomicInteger();
        for (String part : parts) {
            if (!literals.isEmpty()) {
                for (Map.Entry<String, String> e : literals.entrySet()) {
                    if (part.contains(e.getKey())) {
                        part = part.replace(e.getKey(), e.getValue());
                    }
                }
            }
            if (queryPartAdapter != null) {
                part = queryPartAdapter.apply(part);
            }
            Filter f = parsePart(part, idx::incrementAndGet);
            if (f != null) {
                filters.add(f);
            }
        }
        return new Filters(filters);
    }

    private Filter parsePart(String part, Supplier<Integer> idxSupplier) {
        for (Definition def : defs) {
            Filter f = def.from(part, idxSupplier);
            if (f != null) {
                return f;
            }
        }
        return null;
    }

    public static class Definition {

        final String hqlName;
        final List<Operator> supportedOperators;
        final Operator syntheticOperator;
        final Predicate<String> nameMatcher;
        final BiFunction<String, Operator, Object> toValue;
        final Function<Filter, String> toString;

        Definition(Predicate<String> nameMatcher, String hqlName, List<Operator> supportedOperators,
                BiFunction<String, Operator, Object> toValue, Operator syntheticOperator, Function<Filter, String> toString) {
            if (hqlName != null && (supportedOperators == null || supportedOperators.isEmpty()) && syntheticOperator == null) {
                throw new IllegalArgumentException(
                        Qute.fmt("Invalid filter definition - no operators defined [hqlName={}]", hqlName));
            }
            this.nameMatcher = nameMatcher;
            this.hqlName = hqlName;
            this.supportedOperators = supportedOperators;
            this.toValue = toValue != null ? toValue : (s, o) -> s;
            this.syntheticOperator = syntheticOperator;
            this.toString = toString;
        }

        Filters.Filter from(String part, Supplier<Integer> idxSupplier) {
            if (supportedOperators.isEmpty()) {
                if (nameMatcher.test(part)) {
                    return new Filter(idxSupplier.get(), part, hqlName,
                            syntheticOperator != null ? syntheticOperator.hqlVal() : null,
                            toValue.apply(part, syntheticOperator),
                            toString);
                }
                return null;
            }
            for (Operator op : supportedOperators) {
                int idx = part.indexOf(op.queryVal);
                if (idx > 0) {
                    String nameVal = part.substring(0, idx);
                    String matchedName = nameMatcher.test(nameVal) ? nameVal : null;
                    if (syntheticOperator != null) {
                        op = syntheticOperator;
                    }
                    if (matchedName != null) {
                        String value = part.substring(idx + op.queryVal.length(), part.length());
                        try {
                            Object val = toValue.apply(value, op);
                            if (val != null) {
                                return new Filter(idxSupplier.get(), matchedName, hqlName,
                                        syntheticOperator != null ? syntheticOperator.hqlVal() : op.hqlVal(), val, toString);
                            }
                        } catch (Exception ignored) {
                            LOG.warnf("Unable to convert filter value: %s", value);
                        }
                    }
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return "Definition [hqlName=" + hqlName + "]";
        }

    }

    public static FilterParser.Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private List<DefinitionBuilder> defs = new ArrayList<>();
        private Function<String, String> queryPartAdapter;

        DefinitionBuilder addDef(String name, String... aliases) {
            List<String> names;
            if (aliases.length == 0) {
                names = List.of(name);
            } else {
                List<String> all = new ArrayList<>();
                all.add(name);
                Collections.addAll(all, aliases);
                names = List.copyOf(all);
            }
            return new DefinitionBuilder(names);
        }

        Builder adaptQueryPart(Function<String, String> fun) {
            this.queryPartAdapter = fun;
            return this;
        }

        public FilterParser build() {
            return new FilterParser(defs.stream().map(
                    builder -> new Definition(builder.nameMatcher, builder.hqlName, builder.supportedOperators, builder.toValue,
                            builder.syntheticOperator, builder.toString))
                    .collect(Collectors.toUnmodifiableList()), queryPartAdapter);
        }

        public class DefinitionBuilder {

            private String hqlName;
            private List<FilterParser.Operator> supportedOperators = new ArrayList<>();
            private Predicate<String> nameMatcher;
            private BiFunction<String, Operator, Object> toValue;
            private Operator syntheticOperator;
            private Function<Filter, String> toString;

            DefinitionBuilder(List<String> names) {
                this.nameMatcher = v -> names.contains(v);
            }

            public DefinitionBuilder nameMatcher(Predicate<String> matcher) {
                this.nameMatcher = matcher;
                return this;
            }

            public DefinitionBuilder hqlName(String val) {
                hqlName = val;
                return this;
            }

            public DefinitionBuilder stringOperators() {
                return operator(Operator.EQ).operator(Operator.LIKE)
                        .toString(
                                f -> f.operator.equals(Operator.LIKE.hqlVal())
                                        // lower(name) like lower(:name)
                                        ? new StringBuilder()
                                                .append("lower(")
                                                .append(f.hqlName)
                                                .append(") like lower(:")
                                                .append(f.getParamName())
                                                .append(")").toString()
                                        : f.toDefaultString());
            }

            public DefinitionBuilder numberOperators() {
                return operator(Operator.GE)
                        .operator(Operator.GT)
                        .operator(Operator.LE)
                        .operator(Operator.LT)
                        // make sure the equals operator is the last one (due to collisions with >= and <=)
                        .operator(Operator.EQ);
            }

            public DefinitionBuilder operator(Operator op) {
                supportedOperators.add(op);
                return this;
            }

            public DefinitionBuilder toStrValue() {
                return toValue((s, o) -> {
                    if (s.startsWith("\"") && s.endsWith("\"")) {
                        s = s.substring(1, s.length() - 1);
                    }
                    if (o == Operator.LIKE && !s.contains("%")) {
                        s = "%" + s + "%";
                    }
                    return s;
                });
            }

            public DefinitionBuilder toIntValue() {
                return toValue((s, o) -> Integer.parseInt(s));
            }

            public DefinitionBuilder toBoolValue() {
                return toValue((s, o) -> Boolean.parseBoolean(s));
            }

            public DefinitionBuilder toValue(BiFunction<String, Operator, Object> fun) {
                toValue = fun;
                return this;
            }

            public DefinitionBuilder toValue(Object val) {
                toValue = (s, o) -> val;
                return this;
            }

            public DefinitionBuilder toNullValue() {
                toValue = (s, o) -> null;
                return this;
            }

            public DefinitionBuilder syntheticOperator(Operator operator) {
                syntheticOperator = operator;
                return this;
            }

            public DefinitionBuilder toString(Function<Filter, String> fun) {
                toString = fun;
                return this;
            }

            public FilterParser.Builder done() {
                defs.add(this);
                return Builder.this;
            }

        }
    }

    public enum Operator {

        LIKE("~"),
        EQ("="),
        GT(">"),
        GE(">="),
        LT("<"),
        LE("<=");

        final String queryVal;

        Operator(String queryVal) {
            this.queryVal = queryVal;
        }

        public String hqlVal() {
            if (this == LIKE) {
                return "like";
            }
            return queryVal;
        }

        static FilterParser.Operator from(String value) {
            for (FilterParser.Operator op : values()) {
                if (op.queryVal.equals(value)) {
                    return op;
                }
            }
            throw new IllegalArgumentException();
        }

    }

}