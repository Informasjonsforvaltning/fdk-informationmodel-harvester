package no.fdk.imcat.service;

import lombok.RequiredArgsConstructor;
import no.fdk.imcat.model.InformationModel;
import no.fdk.imcat.model.InformationModelFactory;
import no.fdk.imcat.model.InformationModelHarvestSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;

/*
    Fetch information models and insert or update them in the search index.
 */
@Service
@RequiredArgsConstructor
public class InformationmodelHarvester {

    public static final String API_TYPE = "api";
    public static final String ALTINN_TYPE = "altinn";
    public static final int RETRY_COUNT_API_RETRIEVAL = 20;

    private static final Logger logger = LoggerFactory.getLogger(InformationmodelHarvester.class);

    private final InformationmodelRepository informationmodelRepository;
    private final ApiRegistrationsHarvest apiRegistrationsHarvest;
    private final AltinnHarvest altinnHarvest;
    private final InformationModelFactory informationModelFactory;

    private final AmqpTemplate rabbitTemplate;

    public void harvestAll() {

        logger.debug("Starting harvest of Information Models from our APIs");
        List<InformationModelHarvestSource> modelSources = getAllHarvestSources();
        List<String> idsHarvested = new ArrayList<>();

        for (InformationModelHarvestSource source : modelSources) {
            if (source.sourceType.equals(API_TYPE)) {
                try {
                    InformationModel model = informationModelFactory.createInformationModel(source);
                    informationmodelRepository.save(model);
                    idsHarvested.add(model.getId());
                } catch (Exception e) {
                    logger.error("Error creating or saving InformationModel for harvestSourceUri={}", source.harvestSourceUri, e);
                }
            } else if (source.sourceType.equals(ALTINN_TYPE)) {
                InformationModel model = altinnHarvest.getByServiceCodeAndEdition(source.serviceCode, source.serviceEditionCode);
                model = informationModelFactory.enrichInformationModelFromAltInn(model);

                if (model != null) {
                    informationmodelRepository.save(model);
                    idsHarvested.add(model.getId());
                }
            }

        }

        List<String> idsToDelete = informationmodelRepository.getAllIdsNotHarvested(idsHarvested);
        informationmodelRepository.deleteByIds(idsToDelete);
        updateSearch();
    }

    private void updateSearch() {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();

        payload.put("updatesearch", "informationmodels");

        try {
            rabbitTemplate.convertAndSend("harvester.UpdateSearchTrigger", payload);
            logger.info("Successfully sent harvest message for publisher {}", payload);
        } catch (AmqpException e) {
            logger.error("Failed to send harvest message for publisher {}", payload, e);
        }
    }

    private List<InformationModelHarvestSource> getAllHarvestSources() {
        ArrayList<InformationModelHarvestSource> sources = new ArrayList<>();
        sources.addAll(altinnHarvest.getHarvestSources());
        sources.addAll(apiRegistrationsHarvest.getHarvestSources());
        return sources;
    }
}
