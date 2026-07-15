// repository/elasticsearch/ComplaintElasticsearchRepository.java
package com.vcsm.repository.elasticsearch;

import com.vcsm.model.elasticsearch.ComplaintDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ComplaintElasticsearchRepository extends ElasticsearchRepository<ComplaintDocument, String> {

    @Query("{\"match\": {\"description\": {\"query\": \"?0\", \"fuzziness\": \"AUTO\"}}}")
    List<ComplaintDocument> searchByDescriptionFuzzy(String query);

    @Query("{\"bool\": {\"must\": [{\"match\": {\"description\": \"?0\"}}], \"filter\": [{\"term\": {\"status\": \"?1\"}}]}}")
    List<ComplaintDocument> searchByDescriptionAndStatus(String query, String status);

    @Query("{\"match_phrase_prefix\": {\"description\": \"?0\"}}")
    List<ComplaintDocument> autoCompleteDescription(String prefix);

    @Query("{\"range\": {\"createdAt\": {\"gte\": \"?0\", \"lte\": \"?1\"}}}")
    List<ComplaintDocument> findByDateRange(LocalDateTime start, LocalDateTime end);

    long countByStatus(String status);

    @Query("{\"aggs\": {\"by_status\": {\"terms\": {\"field\": \"status\"}}}}")
    List<Object> getStatusAggregation();

    @Query("{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"residentName^2\", \"description\", \"category\", \"apartmentNumber\"]}}")
    List<ComplaintDocument> multiFieldSearch(String query);
}