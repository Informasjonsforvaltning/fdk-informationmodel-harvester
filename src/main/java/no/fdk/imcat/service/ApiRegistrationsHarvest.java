package no.fdk.imcat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.RequiredArgsConstructor;
import no.fdk.acat.common.model.ApiRegistrationPublic;
import no.fdk.acat.converters.apispecificationparser.OpenApiV3JsonParser;
import no.fdk.imcat.model.InformationModelHarvestSource;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.nio.charset.StandardCharsets.UTF_8;
import static no.fdk.imcat.service.InformationmodelHarvester.API_TYPE;

/**
 * Harvest our own ApiRegistration system so we can create Information Models from those that have schema
 */
@Service
@RequiredArgsConstructor
public class ApiRegistrationsHarvest {

    private static String INFORMATIONMODEL_ROOT = "https://fellesdatakatalog.brreg.no/informationmodels/";
    private static final Logger logger = LoggerFactory.getLogger(ApiRegistrationsHarvest.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final RegistrationApiClient registrationApiClient;

    private String getApiSpec(ApiRegistrationPublic apiRegistration) throws IOException {
        String apiSpecUrl = apiRegistration.getApiSpecUrl();
        String apiSpec = apiRegistration.getApiSpec();

        if (isNullOrEmpty(apiSpec) && !isNullOrEmpty(apiSpecUrl)) {
            apiSpec = IOUtils.toString(new URL(apiSpecUrl).openStream(), UTF_8);
        }
        return apiSpec;
    }

    List<InformationModelHarvestSource> getHarvestSources() {
        List<InformationModelHarvestSource> sourceList = new ArrayList<>();
        List<ApiRegistrationPublic> apiRegistrations = registrationApiClient.getPublished();
        for (ApiRegistrationPublic apiRegistration : apiRegistrations) {
            logger.debug("Start importing from ApiRegistration, id={}", apiRegistration.getId());

//            1) get spec
//            2) parse spec to OpenApi
//            3) if contains schemas
//            4) recursively replace refs in schemas.

            try {
                String apiSpec = getApiSpec(apiRegistration);
                OpenApiV3JsonParser parser = new OpenApiV3JsonParser();
                OpenAPI openAPI = parser.parseToOpenAPI(apiSpec);

                if (openAPI.getComponents() == null || openAPI.getComponents().getSchemas() == null) {
                    logger.info("Skip import, no component schemas found in api spec id={}", apiRegistration.getId());
                    continue;
                }
                InformationModelHarvestSource hs = new InformationModelHarvestSource();
                String harvestSourceUri = registrationApiClient.getPublicApisUrlBase() + '/' + apiRegistration.getId();
                hs.harvestSourceUri = harvestSourceUri;
                hs.publisherOrgNr = apiRegistration.getCatalogId();
                hs.sourceType = API_TYPE;
                hs.title = openAPI.getInfo().getTitle();
                hs.schema = extractSchemaFromOpenApi(apiSpec, apiRegistration.getId());
                sourceList.add(hs);

            } catch (Throwable e) {
                logger.info("Skipping api registration, id={}, reason:{}", apiRegistration.getId(), e.getMessage());
            }
        }
        return sourceList;
    }

    private String extractSchemaFromOpenApi(String openApiSpec, String id) throws IOException {

        /*
        informationmodel schema is defined as:
        {
            $schema:<jsonschema version ref>
            $id:<url>
            definitions:{[name]:<schema>}
        }
         */

        JsonNode componentsSchemasNode = mapper.readTree(openApiSpec).path("components").path("schemas");

        if (componentsSchemasNode == null || componentsSchemasNode.size() < 1) {
            return null;
        }

        String componentsSchemasJson = mapper.writeValueAsString(componentsSchemasNode);
        String definitionsJson = componentsSchemasJson.replace("#/components/schemas/", "#/definitions/");
        JsonNode definitionsNode = mapper.readTree(definitionsJson);

        ObjectNode JSONSchemaRootNode = mapper.createObjectNode();
        String schemaId = INFORMATIONMODEL_ROOT + id + "/schema";

        JSONSchemaRootNode.put("$schema", "http://json-schema.org/draft-06/schema#");
        JSONSchemaRootNode.put("$id", schemaId);
        JSONSchemaRootNode.set("definitions", definitionsNode);
        ObjectWriter writer = mapper.writer();

        return writer.writeValueAsString(JSONSchemaRootNode);
    }
}
