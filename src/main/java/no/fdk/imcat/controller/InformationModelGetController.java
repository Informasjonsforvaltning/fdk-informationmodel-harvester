package no.fdk.imcat.controller;

import lombok.RequiredArgsConstructor;
import no.fdk.imcat.dto.InformationModel;
import no.fdk.imcat.model.InformationModelEnhanced;
import no.fdk.imcat.service.InformationModelMapper;
import no.fdk.imcat.service.InformationmodelEnhancedRepository;
import no.fdk.imcat.service.InformationmodelRepository;
import no.fdk.webutils.exceptions.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/informationmodels")
public class InformationModelGetController {
    private static final Logger logger = LoggerFactory.getLogger(InformationModelGetController.class);

    private final InformationmodelRepository informationmodelRepository;
    private final InformationmodelEnhancedRepository informationmodelEnhancedRepository;
    private final InformationModelMapper informationModelMapper;

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public InformationModel getInformationModelEnhanced(@PathVariable String id) throws NotFoundException {
        Optional<no.fdk.imcat.model.InformationModel> oldModel = informationmodelRepository.findById(id);
        if (oldModel.isPresent()) {
            no.fdk.imcat.model.InformationModel informationModel = oldModel.get();
            logger.debug("Retrieved old model {}", informationModel.getId());
            return informationModelMapper.convertModel(informationModel);
        }
        InformationModelEnhanced informationModelEnhanced = informationmodelEnhancedRepository.findById(id).orElseThrow(NotFoundException::new);
        logger.debug("Retrieved new model {}", informationModelEnhanced.getId());
        return informationModelMapper.convertModel(informationModelEnhanced);
    }
}
