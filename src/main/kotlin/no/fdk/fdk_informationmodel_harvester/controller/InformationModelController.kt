package no.fdk.fdk_informationmodel_harvester.controller

import no.fdk.fdk_informationmodel_harvester.rdf.JenaType
import no.fdk.fdk_informationmodel_harvester.rdf.jenaTypeFromAcceptHeader
import no.fdk.fdk_informationmodel_harvester.service.InformationModelService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

private val LOGGER = LoggerFactory.getLogger(InformationModelController::class.java)

@Controller
@CrossOrigin
@RequestMapping(value = ["/informationmodels"], produces = ["text/turtle", "text/n3", "application/rdf+json", "application/ld+json", "application/rdf+xml", "application/n-triples"])
open class InformationModelController(private val informationModelService: InformationModelService) {

    @GetMapping(value = ["/{id}"])
    fun getInformationModelById(
        httpServletRequest: HttpServletRequest,
        @PathVariable id: String,
        @RequestParam(value = "catalogrecords", required = false) catalogrecords: Boolean = false
    ): ResponseEntity<String> {
        LOGGER.info("get InformationModel with id $id")
        val returnType = jenaTypeFromAcceptHeader(httpServletRequest.getHeader("Accept"))

        return if (returnType == JenaType.NOT_JENA) ResponseEntity(HttpStatus.NOT_ACCEPTABLE)
        else {
            informationModelService.getInformationModelById(id, returnType ?: JenaType.TURTLE, catalogrecords)
                ?.let { ResponseEntity(it, HttpStatus.OK) }
                ?: ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

}