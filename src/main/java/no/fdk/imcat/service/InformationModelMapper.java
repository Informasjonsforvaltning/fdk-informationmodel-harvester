package no.fdk.imcat.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import no.dcat.shared.HarvestMetadata;
import no.fdk.imcat.dto.Harvest;
import no.fdk.imcat.dto.InformationModel;
import no.fdk.imcat.dto.ModelDescription;
import no.fdk.imcat.dto.Node;
import no.fdk.imcat.model.InformationModelDocument;
import no.fdk.imcat.model.InformationModelEnhanced;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InformationModelMapper {

    public InformationModel convertModel(InformationModelEnhanced enhancedModel) {
        InformationModel dto = new InformationModel();

        dto.setId(enhancedModel.getId());
        dto.setPublisher(enhancedModel.getPublisher());
        dto.setHarvestSourceUri(enhancedModel.getHarvestSourceUri());
        dto.setHarvest(enhancedModel.getHarvest());
        dto.setTitle(enhancedModel.getTitle());

        Map<String, List<Node>> types = enhancedModel.getDocument().getTypes()
                .stream().collect(Collectors.groupingBy(Node::getModelElementType));

        dto.setObjectTypes(types.get("objekttype"));
        dto.setSimpleTypes(types.get("enkeltype"));
        dto.setCodeTypes(types.get("kodeliste"));
        dto.setDataTypes(types.get("datatype"));

        InformationModelDocument document = enhancedModel.getDocument();
        ModelDescription description = new ModelDescription();
        description.setName(document.getName());
        description.setDescription(document.getDescription());
        description.setThemes(document.getThemes());

        dto.setInformationModelDescription(description);
        return dto;
    }

    public InformationModel convertModel(no.fdk.imcat.model.InformationModel oldModel) {
        InformationModel dto = new InformationModel();
        dto.setId(oldModel.getId());
        dto.setTitle(convertToDefaultLanguage(oldModel.getTitle()));
        dto.setSchema(oldModel.getSchema());
        dto.setHarvest(convertHarvestMetadata(oldModel.getHarvest()));
        dto.setPublisher(oldModel.getPublisher());
        dto.setHarvestSourceUri(oldModel.getHarvestSourceUri());
        return dto;
    }

    private LocalDate toLocalDate(Date date) {
        return date != null ? date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null;
    }

    private Harvest convertHarvestMetadata(HarvestMetadata harvestMetadata) {
        Harvest harvestDto = new Harvest();
        harvestDto.setFirstHarvested(toLocalDate(harvestMetadata.getFirstHarvested()));
        harvestDto.setLastHarvested(toLocalDate(harvestMetadata.getLastHarvested()));
        harvestDto.setLastChanged(toLocalDate(harvestMetadata.getLastChanged()));
        return harvestDto;
    }

    private Map<String, String> convertToDefaultLanguage(String language) {
        return Collections.singletonMap(Language.NB.value, language);
    }

    @Getter
    @AllArgsConstructor
    private enum Language {
        NB("nb");

        private String value;
    }
}
