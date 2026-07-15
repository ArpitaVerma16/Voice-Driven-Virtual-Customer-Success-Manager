// model/elasticsearch/ElasticsearchPage.java
package com.vcsm.model.elasticsearch;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ElasticsearchPage<T> {
    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int pageNumber;
    private int pageSize;
    private boolean last;
    private boolean first;

    public static <T> ElasticsearchPage<T> empty() {
        return ElasticsearchPage.<T>builder()
            .content(List.of())
            .totalElements(0)
            .totalPages(0)
            .pageNumber(0)
            .pageSize(0)
            .last(true)
            .first(true)
            .build();
    }
}