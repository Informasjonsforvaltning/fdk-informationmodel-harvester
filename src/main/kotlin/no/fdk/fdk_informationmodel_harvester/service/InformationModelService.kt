package no.fdk.fdk_informationmodel_harvester.service

import no.fdk.fdk_informationmodel_harvester.harvester.formatNowWithOsloTimeZone
import no.fdk.fdk_informationmodel_harvester.model.DuplicateIRI
import no.fdk.fdk_informationmodel_harvester.model.FdkIdAndUri
import no.fdk.fdk_informationmodel_harvester.model.HarvestReport
import no.fdk.fdk_informationmodel_harvester.rabbit.RabbitMQPublisher
import no.fdk.fdk_informationmodel_harvester.rdf.createRDFResponse
import no.fdk.fdk_informationmodel_harvester.rdf.parseRDFResponse
import no.fdk.fdk_informationmodel_harvester.repository.InformationModelRepository
import org.apache.jena.riot.Lang
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class InformationModelService(
    private val informationModelRepository: InformationModelRepository,
    private val rabbitPublisher: RabbitMQPublisher,
    private val turtleService: TurtleService
) {

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

    fun removeDuplicates(duplicates: List<DuplicateIRI>) {
        val start = formatNowWithOsloTimeZone()
        val reportAsRemoved: MutableList<FdkIdAndUri> = mutableListOf()

        duplicates.flatMap { duplicate ->
            val remove = informationModelRepository.findByIdOrNull(duplicate.iriToRemove)
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No information model connected to IRI ${duplicate.iriToRemove}")

            val retain = informationModelRepository.findByIdOrNull(duplicate.iriToRetain)
                ?.let { if (it.issued > remove.issued) it.copy(issued = remove.issued) else it } // keep earliest issued
                ?.let { if (it.modified < remove.modified) it.copy(modified = remove.modified) else it } // keep latest modified
                ?.let {
                    if (duplicate.keepRemovedFdkId) {
                        if (it.removed) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Information model with IRI ${it.uri} has already been removed")
                        reportAsRemoved.add(FdkIdAndUri(fdkId = it.fdkId, uri = it.uri))
                        it.copy(fdkId = remove.fdkId)
                    } else {
                        if (remove.removed) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Information model with IRI ${remove.uri} has already been removed")
                        reportAsRemoved.add(FdkIdAndUri(fdkId = remove.fdkId, uri = remove.uri))
                        it
                    }
                }
                ?: remove.copy(uri = duplicate.iriToRetain)

            listOf(remove.copy(removed = true), retain.copy(removed = false))
        }.run { informationModelRepository.saveAll(this) }

        if (reportAsRemoved.isNotEmpty()) {
            rabbitPublisher.send(listOf(
                HarvestReport(
                    id = "duplicate-delete",
                    url = "https://fellesdatakatalog.digdir.no/duplicates",
                    harvestError = false,
                    startTime = start,
                    endTime = formatNowWithOsloTimeZone(),
                    removedResources = reportAsRemoved
                )
            ))
        }
    }

    // Purges everything associated with a removed fdkID
    fun purgeByFdkId(fdkId: String) {
        informationModelRepository.findAllByFdkId(fdkId)
            .also { infoModels -> if (infoModels.any { !it.removed }) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to purge files, information model with id $fdkId has not been removed") }
            .run { informationModelRepository.deleteAll(this) }

        turtleService.deleteTurtleFiles(fdkId)
    }

}
