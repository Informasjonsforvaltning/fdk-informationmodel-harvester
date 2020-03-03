package no.fdk.imcat.controller;

import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import no.fdk.imcat.dto.InformationModel;
import no.fdk.imcat.service.InformationModelSearchService;
import no.fdk.webutils.aggregation.PagedResourceWithAggregations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/informationmodels")
public class InformationModelSearchController {
    private static final Logger logger = LoggerFactory.getLogger(InformationModelSearchController.class);

    private final InformationModelSearchService informationModelSearchService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public PagedResourceWithAggregations<InformationModel> searchNew(
            @ApiParam("The query text")
            @RequestParam(value = "q", defaultValue = "", required = false)
                    String query,

            @ApiParam("Filters on publisher's organization path (orgPath), e.g. STAT/972417858/971040238")
            @RequestParam(value = "orgPath", defaultValue = "", required = false)
                    String orgPath,

            @ApiParam("Filters on harvestSourceUri external identifier")
            @RequestParam(value = "harvestSourceUri", defaultValue = "", required = false)
                    String harvestSourceUri,

            @ApiParam("Los theme filter")
            @RequestParam(value = "losTheme", required = false)
                    Set<String> losThemes,

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

            @RequestParam(value = "conceptUris", required = false)
                    Set<String> conceptUris,

            @PageableDefault()
                    Pageable pageable
    ) {
        return informationModelSearchService.search(
                query,
                orgPath,
                harvestSourceUri,
                losThemes,
                aggregations,
                returnFields,
                sortfield,
                sortdirection,
                conceptUris,
                pageable
        );
    }
}
