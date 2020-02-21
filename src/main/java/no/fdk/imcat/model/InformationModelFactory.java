package no.fdk.imcat.model;

import lombok.RequiredArgsConstructor;
import no.dcat.shared.HarvestMetadata;
import no.dcat.shared.HarvestMetadataUtil;
import no.dcat.shared.Publisher;
import no.fdk.imcat.service.InformationmodelRepository;
import no.fdk.imcat.service.OrganizationCatalogueClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InformationModelFactory {
    private static final Logger logger = LoggerFactory.getLogger(InformationModelFactory.class);
    private final OrganizationCatalogueClient organizationCatalogueClient;
    private final InformationmodelRepository informationmodelRepository;

    public InformationModel createInformationModel(InformationModelHarvestSource source, Date harvestDate) {

        Optional<InformationModel> existingModelOptional = informationmodelRepository.getByHarvestSourceUri(source.harvestSourceUri);

        String id = existingModelOptional.isPresent() ? existingModelOptional.get().getId() : UUID.randomUUID().toString();

        InformationModel model = new InformationModel();
        model.setHarvestSourceUri(source.harvestSourceUri);
        model.setId(id);
        model.setTitle(source.title);
        model.setSchema(source.schema);
        model.setPublisher(lookupPublisher(source.publisherOrgNr));

        updateHarvestMetadata(model, harvestDate, existingModelOptional.orElse(null));

        return model;
    }

    public InformationModel enrichInformationModelFromAltInn(InformationModel model, Date harvestDate) {
        Optional<InformationModel> existingModelOptional = informationmodelRepository.getByHarvestSourceUri(model.getHarvestSourceUri());

        String id = existingModelOptional.isPresent() ? existingModelOptional.get().getId() : UUID.randomUUID().toString();
        model.setId(id);

        updateHarvestMetadata(model, harvestDate, existingModelOptional.orElse(null));
        return model;
    }

    private Publisher lookupPublisher(String orgNr) {
        return Publisher.from(organizationCatalogueClient.getOrganization(orgNr));
    }

    private void updateHarvestMetadata(InformationModel informationModel, Date harvestDate, InformationModel existingInformationModel) {
        HarvestMetadata oldHarvest = existingInformationModel != null ? existingInformationModel.getHarvest() : null;

        // new document is not considered a change
        boolean hasChanged = existingInformationModel != null && !isEqualContent(informationModel, existingInformationModel);

        HarvestMetadata harvest = HarvestMetadataUtil.createOrUpdate(oldHarvest, harvestDate, hasChanged);

        informationModel.setHarvest(harvest);
    }

    private boolean isEqualContent(InformationModel first, InformationModel second) {
        String[] ignoredProperties = {"id", "harvest"};
        InformationModel firstContent = new InformationModel();
        InformationModel secondContent = new InformationModel();

        BeanUtils.copyProperties(first, firstContent, ignoredProperties);
        BeanUtils.copyProperties(second, secondContent, ignoredProperties);

        // This is a poor mans comparator. Seems to include all fields
        String firstString = firstContent.toString();
        String secondString = secondContent.toString();
        return firstString.equals(secondString);
    }
}
