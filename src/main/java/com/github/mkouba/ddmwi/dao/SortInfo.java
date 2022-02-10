package com.github.mkouba.ddmwi.dao;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map.Entry;

public class SortInfo {

    public final String selected;
    public final List<Entry<String, String>> options;

    public SortInfo(List<Entry<String, String>> options) {
        this(null, options);
    }

    public SortInfo(String selected, List<Entry<String, String>> options) {
        if (selected == null || options.stream().map(Entry::getValue).noneMatch(selected::equals)) {
            selected = options.get(0).getValue();
        }
        this.selected = selected;
        this.options = options;
    }

    public String getEncodedSelected() {
        return URLEncoder.encode(selected, StandardCharsets.UTF_8);
    }

}