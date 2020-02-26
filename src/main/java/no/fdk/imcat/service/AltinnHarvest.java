package no.fdk.imcat.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import no.dcat.shared.Publisher;
import no.fdk.imcat.model.InformationModel;
import no.fdk.imcat.model.InformationModelHarvestSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.zip.GZIPInputStream;

/**
 * Harvest Altinns APIS
 */

@Service
@RequiredArgsConstructor
public class AltinnHarvest {
    private static final Logger logger = LoggerFactory.getLogger(AltinnHarvest.class);
    private final OrganizationCatalogueClient organizationCatalogueClient;
    private ObjectMapper objectMapper = new ObjectMapper();
    @Value("${application.harvestSourceURIBase}")
    private String harvestSourceURIBase;
    private HashMap<String, InformationModel> everyAltinnInformationModel = new HashMap<>();

    private static String extractSingleForm(byte[] gzippedJson) {
        try (ByteArrayInputStream bin = new ByteArrayInputStream(gzippedJson);
             GZIPInputStream gzipper = new GZIPInputStream(bin)) {
            ByteArrayOutputStream myBucket = new ByteArrayOutputStream();
            boolean done = false;
            byte[] buffer = new byte[10000];
            while (!done) {
                int length = gzipper.read(buffer, 0, buffer.length);
                if (length > 0) {
                    myBucket.write(buffer, 0, length);
                }
                done = (length == -1);
            }
            gzipper.close();

            return new String(myBucket.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException ie) {
            logger.debug("Failed to gunzip JSON", ie);
        }
        return null;
    }

    private InformationModel parseInformationModel(AltInnService service) {
        InformationModel model = new InformationModel();

        model.setPublisher(lookupPublisher(service.OrganizationNumber));
        model.setTitle(service.ServiceName);
        model.setHarvestSourceUri(harvestSourceURIBase + service.ServiceCode + "_" + service.ServiceEditionCode);

        StringBuilder formBuilder = new StringBuilder();

        List<String> decodedForms = new ArrayList<>();
        for (AltinnForm form : service.Forms) {
            byte[] gzippedJson = Base64.getDecoder().decode(form.JsonSchema);
            decodedForms.add(extractSingleForm(gzippedJson));
        }
        boolean serviceHasMultipleForms = service.Forms.size() > 1;
        if (serviceHasMultipleForms) {
            //Put all the separate Schemas into a JSON Array
            formBuilder.append("[");
            for (int i = 0; i < decodedForms.size() - 1; i++) {
                formBuilder.append(decodedForms.get(i));
                formBuilder.append(",");
            }
            formBuilder.append(decodedForms.get(decodedForms.size() - 1));
            formBuilder.append("]");
        } else {
            formBuilder.append(decodedForms.get(0));
        }
        model.setSchema(formBuilder.toString());
        return model;
    }

    Publisher lookupPublisher(String orgNr) {
        try {
            return Publisher.from(organizationCatalogueClient.getOrganization(orgNr));
        } catch (Exception e) {
            logger.warn("Publisher lookup failed for orgNr={}. Error: {}", orgNr, e.getMessage());
        }
        return null;
    }

    public InformationModel getByServiceCodeAndEdition(String serviceCode, String serviceEditionCode) {
        return everyAltinnInformationModel.get(serviceCode + "_" + serviceEditionCode);
    }

    List<InformationModelHarvestSource> getHarvestSources() {
        logger.debug("Getting harvest sources from AltInn");
        List<InformationModelHarvestSource> sourceList = new ArrayList<>();

        loadAllInformationModelsFromOurAltInnAdapter();

        for (String key : everyAltinnInformationModel.keySet()) {

            InformationModel model = everyAltinnInformationModel.get(key);

            int underscoreIndex = key.indexOf("_");
            String altInnServiceCode = key.substring(0, underscoreIndex);
            String altInnServiceEditionCode = key.substring(underscoreIndex + 1);

            InformationModelHarvestSource source = new InformationModelHarvestSource();
            source.harvestSourceUri = model.getHarvestSourceUri();
            source.sourceType = InformationmodelHarvester.ALTINN_TYPE;

            source.serviceCode = altInnServiceCode;
            source.serviceEditionCode = altInnServiceEditionCode;
            sourceList.add(source);
        }
        return sourceList;
    }

    private void loadAllInformationModelsFromOurAltInnAdapter() {

        InputStream schemasResource = getClass().getClassLoader().getResourceAsStream("schemas.json");

        if (schemasResource == null) {
            logger.error("could not find the schema resource file");
            return;
        }

        try (Scanner scanner = new Scanner(schemasResource, "UTF-8")) {
            String JSonSchemaFromFile = scanner.useDelimiter("\\A").next();
            List<AltInnService> servicesInAltInn = objectMapper.readValue(JSonSchemaFromFile, new TypeReference<List<AltInnService>>() {});

            //Now extract the subforms from base64 gzipped json
            new ForkJoinPool(10).submit(() -> servicesInAltInn.parallelStream().forEach(service -> {
                InformationModel model = parseInformationModel(service);
                everyAltinnInformationModel.put(model.getHarvestSourceUri(), model);
            })).join();
        } catch (FileNotFoundException e) {
            logger.error("Schema file is missing", e);
        } catch (Throwable e) {
            logger.error("Failed while reading information models from  ", e);
        }
    }

    private static class AltInnService {

        public String ServiceOwnerCode;
        public String ServiceOwnerName;
        public String OrganizationNumber;
        public String ServiceName;
        public String ServiceCode;
        public String ServiceEditionCode;
        public String ValidFrom;
        public String ValidTo;
        public String ServiceType;
        public String EnterpriseUserEnabled;
        public List<AltinnForm> Forms;

        AltInnService() {
        }
    }

    private static class AltinnForm {
        public String FormID;
        public String FormName;
        public String FormType;
        public String DataFormatID;
        public String DataFormatVersion;
        public String XsdSchemaUrl;
        public String JsonSchema;
    }

}
