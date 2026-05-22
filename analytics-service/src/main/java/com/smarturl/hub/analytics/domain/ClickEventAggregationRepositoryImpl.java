package com.smarturl.hub.analytics.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ClickEventAggregationRepositoryImpl implements ClickEventAggregationRepository {

    private static final String COLLECTION = "click_events";
    private static final String FIELD_LINK_ID = "linkId";
    private static final String FIELD_TIMESTAMP = "timestamp";
    private static final String FIELD_COUNTRY = "country";
    private static final String CLICKS = "clicks";


    private final MongoTemplate mongoTemplate;

    @Override
    public List<DailyClickCount> aggregateDailyCounts(UUID linkId, Instant from, Instant to) {
        var match = Aggregation.match(Criteria.where(FIELD_LINK_ID).is(linkId)
                .and(FIELD_TIMESTAMP).gte(Date.from(from)).lt(Date.from(to)));
        var project = Aggregation.project()
                .and(DateOperators.DateToString.dateOf(FIELD_TIMESTAMP)
                        .toString("%Y-%m-%d")
                        .withTimezone(DateOperators.Timezone.valueOf("UTC")))
                .as("day");
        var group = Aggregation.group("day").count().as(CLICKS);
        var sort = Aggregation.sort(Sort.by("_id"));

        var aggregation = Aggregation.newAggregation(match, project, group, sort);
        var results = mongoTemplate.aggregate(aggregation, COLLECTION, Document.class).getMappedResults();
        return results.stream()
                .map(doc -> new DailyClickCount(
                        LocalDate.parse(doc.getString("_id")),
                        ((Number) doc.get(CLICKS)).longValue()))
                .toList();
    }

    @Override
    public List<CountryClickCount> aggregateCountryCounts(UUID linkId) {
        var match = Aggregation.match(Criteria.where(FIELD_LINK_ID).is(linkId));
        var group = Aggregation.group(FIELD_COUNTRY).count().as(CLICKS);
        var sort = Aggregation.sort(Sort.by(Sort.Direction.DESC, CLICKS));

        var aggregation = Aggregation.newAggregation(match, group, sort);
        var results = mongoTemplate.aggregate(aggregation, COLLECTION, Document.class).getMappedResults();
        return results.stream()
                .map(doc -> new CountryClickCount(
                        doc.getString("_id"),
                        ((Number) doc.get(CLICKS)).longValue()))
                .toList();
    }
}
