package no.fdk.imcat.controller;

import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import no.fdk.imcat.dto.InformationModelDto;
import no.fdk.imcat.model.InformationModel;
import no.fdk.imcat.model.InformationModelEnhanced;
import no.fdk.imcat.service.InformationModelMapper;
import no.fdk.imcat.service.InformationmodelEnhancedRepository;
import no.fdk.imcat.service.InformationmodelRepository;
import no.fdk.webutils.exceptions.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @ApiOperation(value = "Get a specific Informationmodel", response = InformationModel.class)
    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = "application/json")
    public InformationModel getInformationModel(@PathVariable String id) throws NotFoundException {
        return informationmodelRepository.findById(id).orElseThrow(NotFoundException::new);
    }

    @RequestMapping(value = "/v2/{id}", method = RequestMethod.GET, produces = "application/json")
    public InformationModelDto getInformationModelEnhanced(@PathVariable String id) throws NotFoundException {
        Optional<InformationModel> oldModel = informationmodelRepository.findById(id);
        if (oldModel.isPresent()) {
            InformationModel informationModel = oldModel.get();
            logger.info("Retrieved old model {}", informationModel.getId());
            return informationModelMapper.convertModel(informationModel);
        }
        InformationModelEnhanced informationModelEnhanced = informationmodelEnhancedRepository.findById(id).orElseThrow(NotFoundException::new);
        logger.info("Retrieved new model {}", informationModelEnhanced.getId());
        return informationModelMapper.convertModel(informationModelEnhanced);
    }
}
