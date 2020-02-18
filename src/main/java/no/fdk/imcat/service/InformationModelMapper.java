package no.fdk.imcat.service;

import lombok.RequiredArgsConstructor;
import no.fdk.imcat.dto.InformationModelDocumentDto;
import no.fdk.imcat.dto.ModelDescriptionDto;
import no.fdk.imcat.dto.Node;
import no.fdk.imcat.model.InformationModel;
import no.fdk.imcat.model.InformationModelDocument;
import no.fdk.imcat.model.InformationModelEnhanced;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InformationModelMapper {
    public InformationModelDocumentDto convertModel(InformationModelEnhanced enhancedModel) {
        InformationModelDocumentDto dto = new InformationModelDocumentDto();

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
        ModelDescriptionDto description = new ModelDescriptionDto();
        description.setName(document.getName());
        description.setDescription(document.getDescription());
        description.setThemes(document.getThemes());

        dto.setInformationModelDescription(description);
        return dto;
    }

    public InformationModelDocumentDto convertModel(InformationModel oldModel) {
        return null;
    }
}
