package no.fdk.fdk_informationmodel_harvester.controller

import no.fdk.fdk_informationmodel_harvester.rdf.jenaTypeFromAcceptHeader
import no.fdk.fdk_informationmodel_harvester.service.InformationModelService
import org.apache.jena.riot.Lang
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

private val LOGGER = LoggerFactory.getLogger(CatalogsController::class.java)

@Controller
@CrossOrigin
@RequestMapping(
    value = ["/catalogs"],
    produces = ["text/turtle", "text/n3", "application/rdf+json", "application/ld+json", "application/rdf+xml",
        "application/n-triples", "application/n-quads", "application/trig", "application/trix"]
)
open class CatalogsController(private val informationModelService: InformationModelService) {

    @GetMapping("/{id}")
    fun getCatalogById(
        @RequestHeader(HttpHeaders.ACCEPT) accept: String?,
        @PathVariable id: String,
        @RequestParam(value = "catalogrecords", required = false) catalogrecords: Boolean = false
    ): ResponseEntity<String> {
        LOGGER.info("get InformationModel catalog with id $id")
        val returnType = jenaTypeFromAcceptHeader(accept)

        return if (returnType == Lang.RDFNULL) ResponseEntity(HttpStatus.NOT_ACCEPTABLE)
        else {
            informationModelService.getCatalogById(id, returnType ?: Lang.TURTLE, catalogrecords)
                ?.let { ResponseEntity(it, HttpStatus.OK) }
                ?: ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    @GetMapping
    fun getCatalogs(
        @RequestHeader(HttpHeaders.ACCEPT) accept: String?,
        @RequestParam(value = "catalogrecords", required = false) catalogrecords: Boolean = false
    ): ResponseEntity<String> =
        ResponseEntity(HttpStatus.MOVED_PERMANENTLY)
}
