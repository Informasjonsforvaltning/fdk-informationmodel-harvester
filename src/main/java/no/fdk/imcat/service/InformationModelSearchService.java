package no.fdk.imcat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import no.fdk.imcat.dto.InformationModel;
import no.fdk.imcat.model.InformationModelEnhanced;
import no.fdk.webutils.aggregation.PagedResourceWithAggregations;
import no.fdk.webutils.aggregation.ResponseUtil;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SourceFilter;
import org.springframework.hateoas.PagedResources;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InformationModelSearchService {
    private static final Logger logger = LoggerFactory.getLogger(InformationModelSearchService.class);
    private static final String MISSING = "MISSING";
    private final ElasticsearchTemplate elasticsearchTemplate;
    private final InformationModelMapper modelMapper;
    private final ObjectMapper objectMapper;

    private static boolean containsExactlyOneWord(String haystack) {
        return Pattern.compile("^\\w+$").matcher(haystack).find();
    }

    public PagedResourceWithAggregations<InformationModel> search(
            String query,
            String orgPath,
            String harvestSourceUri,
            Set<String> aggregations,
            String[] returnFields,
            String sortfield,
            String sortdirection,
            Pageable pageable) {
        logger.debug("GET /informationmodels/v2/?q={}", query);

        QueryBuilder searchQuery;

        if (query.isEmpty()) {
            searchQuery = QueryBuilders.matchAllQuery();
        } else {
            if (containsExactlyOneWord(query)) {
                query = query.concat(String.format(" %s*", query));
            }
            searchQuery = QueryBuilders.simpleQueryStringQuery(query);
        }

        BoolQueryBuilder composedQuery = QueryBuilders.boolQuery().must(searchQuery);

        if (!orgPath.isEmpty()) {
            composedQuery.filter(QueryUtil.createTermQuery("publisher.orgPath", orgPath));
        }

        if (!harvestSourceUri.isEmpty()) {
            composedQuery.filter(QueryUtil.createTermQuery("harvestSourceUri", harvestSourceUri));
        }

        NativeSearchQuery finalQuery = new NativeSearchQueryBuilder()
                .withQuery(composedQuery)
                .withIndices("imcat", "imcatenh").withTypes("informationmodel", "informationmodelenhanced")
                .withPageable(pageable)
                .build();

        if (aggregations != null) {
            if (aggregations.contains("orgPath")) {
                finalQuery.addAggregation(AggregationBuilders
                        .terms("orgPath")
                        .field("publisher.orgPath")
                        .missing(MISSING)
                        .size(Integer.MAX_VALUE)
                        .order(Terms.Order.count(false)));
            }
            if (aggregations.contains("los")) {
                finalQuery.addAggregation(AggregationBuilders
                        .terms("los")
                        .field("document.themes.losPaths")
                        .missing(MISSING)
                        .size(Integer.MAX_VALUE)
                        .order(Terms.Order.count(false)));
            }
        }

        if (returnFields != null && returnFields.length > 0) {
            SourceFilter sourceFilter = new FetchSourceFilter(returnFields, null);
            finalQuery.addSourceFilter(sourceFilter);
        }

        if (sortfield.equals("modified")) {
            Sort.Direction sortOrder = sortdirection.toLowerCase().contains("asc".toLowerCase()) ? Sort.Direction.ASC : Sort.Direction.DESC;
            String sortProperty = "harvest.firstHarvested";
            finalQuery.addSort(new Sort(sortOrder, sortProperty));
        }

        AggregatedPage<JsonNode> aggregatedPage = elasticsearchTemplate.queryForPage(finalQuery, JsonNode.class);

        List<InformationModel> informationModels = aggregatedPage.getContent().stream().map(j -> {
            try {
                if (j.has("document")) {
                    return modelMapper.convertModel(objectMapper.treeToValue(j, InformationModelEnhanced.class));
                }
                return modelMapper.convertModel(objectMapper.treeToValue(j, no.fdk.imcat.model.InformationModel.class));
            } catch (JsonProcessingException e) {
                logger.error("could not deserialize model", e);
            }
            return null;
        }).collect(Collectors.toList());

        PagedResources.PageMetadata pageMetadata = new PagedResources.PageMetadata(
                pageable.getPageSize(),
                pageable.getPageNumber(),
                aggregatedPage.getTotalElements(),
                aggregatedPage.getTotalPages()
        );

        return new PagedResourceWithAggregations<>(
                new PagedResources<>(informationModels, pageMetadata),
                ResponseUtil.extractAggregations(aggregatedPage)
        );
    }

    static class QueryUtil {
        static QueryBuilder createTermQuery(String term, String value) {
            return value.equals(MISSING) ?
                    QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(term)) :
                    QueryBuilders.termQuery(term, value);
        }
    }
}
