package no.fdk.fdk_informationmodel_harvester.harvester

import no.fdk.fdk_informationmodel_harvester.configuration.ApplicationProperties
import no.fdk.fdk_informationmodel_harvester.adapter.InformationModelAdapter
import no.fdk.fdk_informationmodel_harvester.model.*
import no.fdk.fdk_informationmodel_harvester.rdf.*
import no.fdk.fdk_informationmodel_harvester.repository.*
import no.fdk.fdk_informationmodel_harvester.service.TurtleService
import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.Lang
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*

private val LOGGER = LoggerFactory.getLogger(InformationModelHarvester::class.java)

@Service
class InformationModelHarvester(
    private val adapter: InformationModelAdapter,
    private val catalogRepository: CatalogRepository,
    private val informationModelRepository: InformationModelRepository,
    private val turtleService: TurtleService,
    private val applicationProperties: ApplicationProperties
) {

    fun harvestInformationModelCatalog(source: HarvestDataSource, harvestDate: Calendar, forceUpdate: Boolean): HarvestReport? =
        if (source.id != null && source.url != null) {
            try {
                LOGGER.debug("Starting harvest of ${source.url}")

                when (val jenaWriterType = jenaTypeFromAcceptHeader(source.acceptHeaderValue)) {
                    null -> {
                        LOGGER.error(
                            "Not able to harvest from ${source.url}, no accept header supplied",
                            HarvestException(source.url)
                        )
                        HarvestReport(
                            id = source.id,
                            url = source.url,
                            harvestError = true,
                            errorMessage = "Not able to harvest, no accept header supplied",
                            startTime = harvestDate.formatWithOsloTimeZone(),
                            endTime = formatNowWithOsloTimeZone()
                        )
                    }
                    Lang.RDFNULL -> {
                        LOGGER.error(
                            "Not able to harvest from ${source.url}, header ${source.acceptHeaderValue} is not acceptable",
                            HarvestException(source.url)
                        )
                        HarvestReport(
                            id = source.id,
                            url = source.url,
                            harvestError = true,
                            errorMessage = "Not able to harvest, no accept header supplied",
                            startTime = harvestDate.formatWithOsloTimeZone(),
                            endTime = formatNowWithOsloTimeZone()
                        )
                    }
                    else -> updateIfChanged(
                        parseRDFResponse(adapter.getInformationModels(source), jenaWriterType),
                        source.id, source.url, harvestDate, forceUpdate
                    )
                }
            } catch (ex: Exception) {
                LOGGER.error("Harvest of ${source.url} failed", ex)
                HarvestReport(
                    id = source.id,
                    url = source.url,
                    harvestError = true,
                    errorMessage = ex.message,
                    startTime = harvestDate.formatWithOsloTimeZone(),
                    endTime = formatNowWithOsloTimeZone()
                )
            }
        } else {
            LOGGER.error("Harvest source is not valid", HarvestException("source not valid"))
            null
        }

    private fun updateIfChanged(harvested: Model, sourceId: String, sourceURL: String, harvestDate: Calendar, forceUpdate: Boolean): HarvestReport {
        val dbData = turtleService.findOne(sourceURL)
            ?.let { safeParseRDF(it, Lang.TURTLE) }

        return if (!forceUpdate && dbData != null && harvested.isIsomorphicWith(dbData)) {
            LOGGER.info("No changes from last harvest of $sourceURL")
            HarvestReport(
                id = sourceId,
                url = sourceURL,
                harvestError = false,
                startTime = harvestDate.formatWithOsloTimeZone(),
                endTime = formatNowWithOsloTimeZone()
            )
        } else {
            LOGGER.info("Changes detected, saving data from $sourceURL and updating FDK meta data")
            turtleService.saveOne(filename = sourceURL, turtle = harvested.createRDFResponse(Lang.TURTLE))

            updateDB(harvested, sourceId, sourceURL, harvestDate, forceUpdate)
        }
    }

    private fun updateDB(harvested: Model, sourceId: String, sourceURL: String, harvestDate: Calendar, forceUpdate: Boolean): HarvestReport {
        val updatedCatalogs = mutableListOf<CatalogMeta>()
        val updatedModels = mutableListOf<InformationModelMeta>()
        val removedModels = mutableListOf<InformationModelMeta>()
        splitCatalogsFromRDF(harvested, sourceURL)
            .map { Pair(it, catalogRepository.findByIdOrNull(it.resourceURI)) }
            .filter { forceUpdate || it.first.catalogHasChanges(it.second?.fdkId) }
            .forEach {
                val updatedCatalogMeta = it.first.mapToCatalogMeta(harvestDate, it.second)
                catalogRepository.save(updatedCatalogMeta)
                updatedCatalogs.add(updatedCatalogMeta)

                turtleService.saveCatalog(
                    fdkId = updatedCatalogMeta.fdkId,
                    turtle = it.first.harvestedCatalog.createRDFResponse(Lang.TURTLE),
                    withRecords = false
                )

                val fdkUri = "${applicationProperties.catalogUri}/${updatedCatalogMeta.fdkId}"

                it.first.models.forEach { infoModel ->
                    infoModel.updateDBOs(harvestDate, fdkUri, forceUpdate)
                        ?.let { modelMeta -> updatedModels.add(modelMeta) }
                }

                removedModels.addAll(
                    getInfoModelsRemovedThisHarvest(
                        fdkUri,
                        it.first.models.map { infoModel -> infoModel.resourceURI }
                    )
                )
            }

        removedModels.map { it.copy(removed = true) }.run { informationModelRepository.saveAll(this) }

        LOGGER.debug("Harvest of $sourceURL completed")
        return HarvestReport(
            id = sourceId,
            url = sourceURL,
            harvestError = false,
            startTime = harvestDate.formatWithOsloTimeZone(),
            endTime = formatNowWithOsloTimeZone(),
            changedCatalogs = updatedCatalogs.map { FdkIdAndUri(fdkId = it.fdkId, uri = it.uri) },
            changedResources = updatedModels.map { FdkIdAndUri(fdkId = it.fdkId, uri = it.uri) },
            removedResources = removedModels.map { FdkIdAndUri(fdkId = it.fdkId, uri = it.uri) }
        )
    }

    private fun InformationModelRDFModel.updateDBOs(
        harvestDate: Calendar,
        fdkCatalogURI: String,
        forceUpdate: Boolean
    ): InformationModelMeta? {
        val dbMeta = informationModelRepository.findByIdOrNull(resourceURI)
        return when {
            dbMeta == null || dbMeta.removed || modelHasChanges(dbMeta.fdkId) -> {
                val updatedMeta = mapToDBOMeta(harvestDate, fdkCatalogURI, dbMeta)
                informationModelRepository.save(updatedMeta)
                turtleService.saveInformationModel(
                    turtle = harvested.createRDFResponse(Lang.TURTLE),
                    fdkId = updatedMeta.fdkId,
                    withRecords = false
                )

                updatedMeta
            }
            forceUpdate -> {
                turtleService.saveInformationModel(
                    turtle = harvested.createRDFResponse(Lang.TURTLE),
                    fdkId = dbMeta.fdkId,
                    withRecords = false
                )
                dbMeta
            }
            else -> null
        }
    }

    private fun CatalogAndInfoModels.mapToCatalogMeta(
        harvestDate: Calendar,
        dbMeta: CatalogMeta?
    ): CatalogMeta {
        val catalogURI = resourceURI
        val fdkId = dbMeta?.fdkId ?: createIdFromString(catalogURI)
        val issued = dbMeta?.issued
            ?.let { timestamp -> calendarFromTimestamp(timestamp) }
            ?: harvestDate

        return CatalogMeta(
            uri = catalogURI,
            fdkId = fdkId,
            issued = issued.timeInMillis,
            modified = harvestDate.timeInMillis
        )
    }

    private fun InformationModelRDFModel.mapToDBOMeta(
        harvestDate: Calendar,
        fdkCatalogURI: String,
        dbMeta: InformationModelMeta?
    ): InformationModelMeta {
        val fdkId = dbMeta?.fdkId ?: createIdFromString(resourceURI)
        val issued: Calendar = dbMeta?.issued
            ?.let { timestamp -> calendarFromTimestamp(timestamp) }
            ?: harvestDate

        return InformationModelMeta(
            uri = resourceURI,
            fdkId = fdkId,
            isPartOf = fdkCatalogURI,
            issued = issued.timeInMillis,
            modified = harvestDate.timeInMillis
        )
    }

    private fun CatalogAndInfoModels.catalogHasChanges(fdkId: String?): Boolean =
        if (fdkId == null) true
        else harvestDiff(turtleService.findCatalog(fdkId, withRecords = false))

    private fun InformationModelRDFModel.modelHasChanges(fdkId: String?): Boolean =
        if (fdkId == null) true
        else harvestDiff(turtleService.findInformationModel(fdkId, withRecords = false))

    private fun getInfoModelsRemovedThisHarvest(catalog: String, infoModels: List<String>): List<InformationModelMeta> =
        informationModelRepository.findAllByIsPartOf(catalog)
            .filter { !it.removed && !infoModels.contains(it.uri) }
}
