package com.github.mkouba.ddmwi.dao;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

public class PageResults<T> {

    static final PageResults<Object> EMPTY = new PageResults<>(Collections.emptyList(), 0, 0);

    @SuppressWarnings("unchecked")
    public static <E> PageResults<E> empty() {
        return (PageResults<E>) EMPTY;
    }

    public static final int DEFAULT_PAGE_SIZE = 18;

    private final List<T> results;
    private final int index;
    private final long count;
    private final int pageSize;
    private final int totalPages;

    public PageResults(List<T> results, int index, long totalCount) {
        this(results, index, totalCount, DEFAULT_PAGE_SIZE);
    }

    public PageResults(List<T> results, int index, long totalCount, int pageSize) {
        this.results = results;
        this.count = totalCount;
        this.pageSize = pageSize;
        this.index = index + 1;
        this.totalPages = Long.valueOf(count / pageSize).intValue() + 1;
    }

    public List<T> getResults() {
        return results;
    }

    public long getCount() {
        return count;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getIndex() {
        return index;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean isEmpty() {
        return results.isEmpty();
    }

    public boolean hasPrevious() {
        return index > 1;
    }

    public int prevIndex() {
        return hasPrevious() ? index - 1 : 0;
    }

    public boolean hasNext() {
        return index < totalPages;
    }

    public int nextIndex() {
        return hasNext() ? index + 1: 0;
    }

    public int[] getVisiblePages() {
        if (totalPages <= 11) {
            return IntStream.range(1, totalPages + 1).toArray();
        }
        // show window of eleven pages
        int left = 5;
        int right = 5;
        if ((index - 5) <= 0) {
            // 1 2 >3< 4 5 6 7 8 9 10 11
            // index=3 => left=2, right=8
            left = 5 - Math.abs(index - 6);
            right = 5 + Math.abs(index - 6);
        } else if ((index + 5) > totalPages) {
            // 18 19 20 21 22 23 24 >25< 26 27 28
            // index=25 => left=7,right=3
            right = totalPages - index;
            left = 5 + right;
        }
       return IntStream.range(index - left, index + right + 1).toArray();
    }
    
    public boolean showFirst() {
        return index - 5 > 1;
    }
    
    public boolean showLast() {
        return index + 5 < totalPages;
    }

}