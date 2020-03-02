package no.fdk.imcat.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import no.fdk.imcat.dto.InformationModel;
import no.fdk.imcat.dto.Node;
import no.fdk.imcat.model.InformationModelDocument;
import no.fdk.imcat.model.InformationModelEnhanced;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InformationModelMapper {

    public InformationModel convertModel(InformationModelEnhanced enhancedModel) {
        InformationModel dto = new InformationModel();
        InformationModelDocument document = enhancedModel.getDocument();
        Map<String, List<Node>> types = enhancedModel.getDocument().getTypes()
                .stream().collect(Collectors.groupingBy(Node::getModelElementType));

        dto.setId(enhancedModel.getId());
        dto.setIdentifier(document.getIdentifier());
        dto.setPublisher(enhancedModel.getPublisher());
        dto.setHarvestSourceUri(enhancedModel.getHarvestSourceUri());
        dto.setHarvest(enhancedModel.getHarvest());
        dto.setTitle(enhancedModel.getTitle());
        dto.setObjectTypes(types.get("objekttype"));
        dto.setSimpleTypes(types.get("enkeltype"));
        dto.setCodeTypes(types.get("kodeliste"));
        dto.setDataTypes(types.get("datatype"));
        dto.setVersion(document.getVersion());
        dto.setValidFromIncluding(document.getValidFromIncluding());
        dto.setValidToIncluding(document.getValidToIncluding());
        dto.setIssued(document.getIssued());
        dto.setLandingPage(document.getLandingPage());
        dto.setCategory(document.getCategory());
        dto.setContactPoint(document.getContactPoint());
        dto.setName(document.getName());
        dto.setDescription(document.getDescription());
        dto.setModelDescription(document.getModelDescription());
        dto.setThemes(document.getThemes());
        dto.setKeywords(document.getKeywords());
        return dto;
    }

    public InformationModel convertModel(no.fdk.imcat.model.InformationModel oldModel) {
        InformationModel dto = new InformationModel();
        dto.setId(oldModel.getId());
        dto.setTitle(convertToDefaultLanguage(oldModel.getTitle()));
        dto.setHarvest(oldModel.getHarvest());
        dto.setSchema(oldModel.getSchema());
        dto.setPublisher(oldModel.getPublisher());
        dto.setHarvestSourceUri(oldModel.getHarvestSourceUri());
        return dto;
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
