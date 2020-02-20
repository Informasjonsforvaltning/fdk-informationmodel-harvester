package no.fdk.imcat.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import no.fdk.imcat.dto.InformationModelDto;
import no.fdk.imcat.model.InformationModel;
import no.fdk.imcat.service.InformationModelSearchService;
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
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.PagedResources;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/informationmodels")
public class InformationModelSearchController {

    private static final String MISSING = "MISSING";
    private static final Logger logger = LoggerFactory.getLogger(InformationModelSearchController.class);
    private final ElasticsearchTemplate elasticsearchTemplate;
    private final InformationModelSearchService informationModelSearchService;

    @ApiOperation(value = "Search in Information Model catalog")
    @RequestMapping(value = "/v2", method = RequestMethod.GET, produces = "application/json")
    public PagedResourceWithAggregations<InformationModelDto> searchNew(
            @ApiParam("The query text")
            @RequestParam(value = "q", defaultValue = "", required = false)
                    String query,

            @ApiParam("Filters on publisher's organization path (orgPath), e.g. /STAT/972417858/971040238")
            @RequestParam(value = "orgPath", defaultValue = "", required = false)
                    String orgPath,

            @ApiParam("Filters on harvestSourceUri external identifier")
            @RequestParam(value = "harvestSourceUri", defaultValue = "", required = false)
                    String harvestSourceUri,

            @ApiParam("Calculate aggregations")
            @RequestParam(value = "aggregations", required = false)
                    Set<String> aggregations,

            @ApiParam("Comma separated list of which fields should be returned. E.g id,")
            @RequestParam(value = "returnfields", required = false)
                    String[] returnFields,

            @ApiParam("Specifies the sort field, at the present the only value is \"modified\". Default is no value, and results are sorted by relevance")
            @RequestParam(value = "sortfield", defaultValue = "", required = false)
                    String sortfield,

            @ApiParam("Specifies the sort direction of the sorted result. The directions are: asc for ascending and desc for descending")
            @RequestParam(value = "sortdirection", defaultValue = "", required = false)
                    String sortdirection,

            @PageableDefault()
                    Pageable pageable
    ) {

        return informationModelSearchService.search(query, orgPath, harvestSourceUri, aggregations, returnFields, sortfield, sortdirection, pageable);
    }

    @ApiOperation(value = "Search in Information Model catalog")
    @RequestMapping(value = "", method = RequestMethod.GET, produces = "application/json")
    public PagedResourceWithAggregations<InformationModel> search(
            @ApiParam("The query text")
            @RequestParam(value = "q", defaultValue = "", required = false)
                    String query,

            @ApiParam("Filters on publisher's organization path (orgPath), e.g. /STAT/972417858/971040238")
            @RequestParam(value = "orgPath", defaultValue = "", required = false)
                    String orgPath,

            @ApiParam("Filters on harvestSourceUri external identifier")
            @RequestParam(value = "harvestSourceUri", defaultValue = "", required = false)
                    String harvestSourceUri,

            @ApiParam("Calculate aggregations")
            @RequestParam(value = "aggregations", required = false)
                    Set<String> aggregations,

            @ApiParam("Comma separated list of which fields should be returned. E.g id,")
            @RequestParam(value = "returnfields", defaultValue = "", required = false)
                    String returnFields,

            @ApiParam("Specifies the sort field, at the present the only value is \"modified\". Default is no value, and results are sorted by relevance")
            @RequestParam(value = "sortfield", defaultValue = "", required = false)
                    String sortfield,

            @ApiParam("Specifies the sort direction of the sorted result. The directions are: asc for ascending and desc for descending")
            @RequestParam(value = "sortdirection", defaultValue = "", required = false)
                    String sortdirection,

            @PageableDefault()
                    Pageable pageable
    ) {
        logger.debug("GET /informationmodels?q={}", query);

        QueryBuilder searchQuery;

        if (query.isEmpty()) {
            searchQuery = QueryBuilders.matchAllQuery();
        } else {
            // add * if query only contains one word
            if (!query.contains(" ")) {
                query = query + " " + query + "*";
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
                .withIndices("imcat").withTypes("informationmodel")
                .withPageable(pageable)
                .build();

        if (aggregations != null && !aggregations.isEmpty()) {
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

        if (isNotEmpty(returnFields)) {
            SourceFilter sourceFilter = new FetchSourceFilter(returnFields.split(","), null);
            finalQuery.addSourceFilter(sourceFilter);
        }

        if ("modified".equals(sortfield)) {
            Sort.Direction sortOrder = sortdirection.toLowerCase().contains("asc".toLowerCase()) ? Sort.Direction.ASC : Sort.Direction.DESC;
            String sortProperty = "harvest.firstHarvested";
            finalQuery.addSort(new Sort(sortOrder, sortProperty));
        }

        AggregatedPage<InformationModel> aggregatedPage = elasticsearchTemplate.queryForPage(finalQuery, InformationModel.class);

        List<InformationModel> informationModels = aggregatedPage.getContent();

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
