package no.fdk.fdk_informationmodel_harvester.controller

import no.fdk.fdk_informationmodel_harvester.rdf.JenaType
import no.fdk.fdk_informationmodel_harvester.rdf.jenaTypeFromAcceptHeader
import no.fdk.fdk_informationmodel_harvester.service.InformationModelService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import javax.servlet.http.HttpServletRequest

private val LOGGER = LoggerFactory.getLogger(CatalogsController::class.java)

@Controller
@CrossOrigin
@RequestMapping(value = ["/catalogs"], produces = ["text/turtle", "text/n3", "application/rdf+json", "application/ld+json", "application/rdf+xml", "application/n-triples"])
open class CatalogsController(private val informationModelService: InformationModelService) {

    @GetMapping(value = ["/{id}"])
    fun getCatalogById(httpServletRequest: HttpServletRequest, @PathVariable id: String): ResponseEntity<String> {
        LOGGER.info("get InformationModel catalog with id $id")
        val returnType = jenaTypeFromAcceptHeader(httpServletRequest.getHeader("Accept"))

        return if (returnType == JenaType.NOT_JENA) ResponseEntity(HttpStatus.NOT_ACCEPTABLE)
        else {
            informationModelService.getCatalogById(id, returnType ?: JenaType.TURTLE)
                ?.let { ResponseEntity(it, HttpStatus.OK) }
                ?: ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    @GetMapping()
    fun getCatalogs(httpServletRequest: HttpServletRequest): ResponseEntity<String> {
        LOGGER.info("get all InformationModel catalogs")
        val returnType = jenaTypeFromAcceptHeader(httpServletRequest.getHeader("Accept"))

        return if (returnType == JenaType.NOT_JENA) ResponseEntity(HttpStatus.NOT_ACCEPTABLE)
        else ResponseEntity(informationModelService.getAll(returnType ?: JenaType.TURTLE), HttpStatus.OK)
    }
}