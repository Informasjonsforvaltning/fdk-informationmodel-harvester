package no.fdk.fdk_informationmodel_harvester.service

import no.fdk.fdk_informationmodel_harvester.harvester.formatNowWithOsloTimeZone
import no.fdk.fdk_informationmodel_harvester.model.FdkIdAndUri
import no.fdk.fdk_informationmodel_harvester.model.HarvestReport
import no.fdk.fdk_informationmodel_harvester.rabbit.RabbitMQPublisher
import no.fdk.fdk_informationmodel_harvester.rdf.createRDFResponse
import no.fdk.fdk_informationmodel_harvester.rdf.parseRDFResponse
import no.fdk.fdk_informationmodel_harvester.repository.InformationModelRepository
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class InformationModelService(
    private val informationModelRepository: InformationModelRepository,
    private val rabbitPublisher: RabbitMQPublisher,
    private val turtleService: TurtleService
) {

    fun getAll(returnType: Lang, includeFDKCatalogRecords: Boolean): String =
        turtleService.findUnionModel(includeFDKCatalogRecords)
            ?.let {
                if (returnType == Lang.TURTLE) it
                else parseRDFResponse(it, Lang.TURTLE).createRDFResponse(returnType)
        } ?: ModelFactory.createDefaultModel().createRDFResponse(returnType)

    fun getInformationModelById(id: String, returnType: Lang, includeFDKCatalogRecords: Boolean): String? =
        turtleService.findInformationModel(id, includeFDKCatalogRecords)
            ?.let {
                if (returnType == Lang.TURTLE) it
                else parseRDFResponse(it, Lang.TURTLE).createRDFResponse(returnType)
            }

    fun getCatalogById(id: String, returnType: Lang, includeFDKCatalogRecords: Boolean): String? =
        turtleService.findCatalog(id, includeFDKCatalogRecords)
            ?.let {
                if (returnType == Lang.TURTLE) it
                else parseRDFResponse(it, Lang.TURTLE).createRDFResponse(returnType)
            }

    fun removeInformationModel(id: String) {
        val start = formatNowWithOsloTimeZone()
        val meta = informationModelRepository.findAllByFdkId(id)
        if (meta.isEmpty()) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "No information model found with fdkID $id")
        } else if (meta.none { !it.removed }) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Information model with fdkID $id has already been removed")
        } else {
            informationModelRepository.saveAll(meta.map { it.copy(removed = true) })

            val uri = meta.first().uri
            rabbitPublisher.send(listOf(
                HarvestReport(
                    id = "manual-delete-$id",
                    url = uri,
                    harvestError = false,
                    startTime = start,
                    endTime = formatNowWithOsloTimeZone(),
                    removedResources = listOf(FdkIdAndUri(fdkId = id, uri = uri))
                )
            ))
        }
    }

}
