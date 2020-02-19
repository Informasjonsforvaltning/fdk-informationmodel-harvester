package no.fdk.webutils.aggregation;

import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ResponseUtil {

    public static <T> Map<String, Aggregation> extractAggregations(AggregatedPage<T> aggregatedPage) {
        if (aggregatedPage.hasAggregations()) {
            Map<String, org.elasticsearch.search.aggregations.Aggregation> aggregationsElastic = aggregatedPage.getAggregations().getAsMap();
            Map<String, Aggregation> aggregations = new HashMap<>();
            aggregationsElastic.forEach((aggregationName, elasticAggregation) -> {
                Aggregation outputAggregation = new Aggregation() {{
                    buckets = new ArrayList<>();
                }};

                ((MultiBucketsAggregation) elasticAggregation).getBuckets().forEach((bucket) -> {
                    outputAggregation.getBuckets().add(AggregationBucket.of(bucket.getKeyAsString(), bucket.getDocCount()));
                });
                aggregations.put(aggregationName, outputAggregation);
            });
            return aggregations;
        }

        return Collections.emptyMap();
    }
}
