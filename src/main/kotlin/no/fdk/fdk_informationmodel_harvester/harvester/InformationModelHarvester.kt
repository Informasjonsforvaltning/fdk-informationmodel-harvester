package no.fdk.fdk_informationmodel_harvester.harvester

import no.fdk.fdk_informationmodel_harvester.configuration.ApplicationProperties
import no.fdk.fdk_informationmodel_harvester.adapter.InformationModelAdapter
import no.fdk.fdk_informationmodel_harvester.model.*
import no.fdk.fdk_informationmodel_harvester.rdf.*
import no.fdk.fdk_informationmodel_harvester.repository.*
import no.fdk.fdk_informationmodel_harvester.service.TurtleService
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.apache.jena.sparql.vocabulary.FOAF
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

private val LOGGER = LoggerFactory.getLogger(InformationModelHarvester::class.java)
private const val dateFormat: String = "yyyy-MM-dd HH:mm:ss Z"

@Service
class InformationModelHarvester(
    private val adapter: InformationModelAdapter,
    private val catalogRepository: CatalogRepository,
    private val informationModelRepository: InformationModelRepository,
    private val turtleService: TurtleService,
    private val applicationProperties: ApplicationProperties
) {

    fun harvestInformationModelCatalog(source: HarvestDataSource, harvestDate: Calendar): HarvestReport? =
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
                        parseRDFResponse(adapter.getInformationModels(source), jenaWriterType, source.url),
                        source.id, source.url, harvestDate
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

    private fun updateIfChanged(harvested: Model, sourceId: String, sourceURL: String, harvestDate: Calendar): HarvestReport {
        val dbData = turtleService.findOne(sourceURL)
            ?.let { parseRDFResponse(it, Lang.TURTLE, null) }

        return if (dbData != null && harvested.isIsomorphicWith(dbData)) {
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

            updateDB(harvested, sourceId, sourceURL, harvestDate)
        }
    }

    private fun updateDB(harvested: Model, sourceId: String, sourceURL: String, harvestDate: Calendar): HarvestReport {
        val updatedCatalogs = mutableListOf<CatalogMeta>()
        val updatedModels = mutableListOf<InformationModelMeta>()
        splitCatalogsFromRDF(harvested, sourceURL)
            .map { Pair(it, catalogRepository.findByIdOrNull(it.resourceURI)) }
            .filter { it.first.catalogHasChanges(it.second?.fdkId) }
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
                    infoModel.updateDBOs(harvestDate, fdkUri)
                        ?.let { modelMeta -> updatedModels.add(modelMeta) }
                }

                var catalogModel = it.first.harvestedCatalogWithoutInfoModels
                catalogModel.createResource(fdkUri)
                    .addProperty(RDF.type, DCAT.CatalogRecord)
                    .addProperty(DCTerms.identifier, updatedCatalogMeta.fdkId)
                    .addProperty(FOAF.primaryTopic, catalogModel.createResource(updatedCatalogMeta.uri))
                    .addProperty(DCTerms.issued, catalogModel.createTypedLiteral(calendarFromTimestamp(updatedCatalogMeta.issued)))
                    .addProperty(DCTerms.modified, catalogModel.createTypedLiteral(harvestDate))

                informationModelRepository.findAllByIsPartOf(fdkUri)
                    .filter { infoMeta -> catalogContainsInfoModel(it.first.harvestedCatalog, updatedCatalogMeta.uri, infoMeta.uri) }
                    .mapNotNull { infoMeta -> turtleService.findInformationModel(infoMeta.fdkId, withRecords = true) }
                    .map { infoModelTurtle -> parseRDFResponse(infoModelTurtle, Lang.TURTLE, null) }
                    .forEach { infoModel -> catalogModel = catalogModel.union(infoModel) }

                turtleService.saveCatalog(
                    fdkId = updatedCatalogMeta.fdkId,
                    turtle = catalogModel.createRDFResponse(Lang.TURTLE),
                    withRecords = true
                )
            }
        LOGGER.debug("Harvest of $sourceURL completed")
        return HarvestReport(
            id = sourceId,
            url = sourceURL,
            harvestError = false,
            startTime = harvestDate.formatWithOsloTimeZone(),
            endTime = formatNowWithOsloTimeZone(),
            changedCatalogs = updatedCatalogs.map { FdkIdAndUri(fdkId = it.fdkId, uri = it.uri) },
            changedResources = updatedModels.map { FdkIdAndUri(fdkId = it.fdkId, uri = it.uri) }
        )
    }

    private fun InformationModelRDFModel.updateDBOs(
        harvestDate: Calendar,
        fdkCatalogURI: String
    ): InformationModelMeta? {
        val dbMeta = informationModelRepository.findByIdOrNull(resourceURI)
        if (modelHasChanges(dbMeta?.fdkId)) {
            val modelMeta = mapToDBOMeta(harvestDate, fdkCatalogURI, dbMeta)
            informationModelRepository.save(modelMeta)

            turtleService.saveInformationModel(
                fdkId = modelMeta.fdkId,
                turtle = harvested.createRDFResponse(Lang.TURTLE),
                withRecords = false
            )

            val fdkUri = "${applicationProperties.informationModelUri}/${modelMeta.fdkId}"
            val metaModel = ModelFactory.createDefaultModel()

            metaModel.createResource(fdkUri)
                .addProperty(RDF.type, DCAT.CatalogRecord)
                .addProperty(DCTerms.identifier, modelMeta.fdkId)
                .addProperty(FOAF.primaryTopic, metaModel.createResource(resourceURI))
                .addProperty(DCTerms.isPartOf, metaModel.createResource(modelMeta.isPartOf))
                .addProperty(DCTerms.issued, metaModel.createTypedLiteral(calendarFromTimestamp(modelMeta.issued)))
                .addProperty(DCTerms.modified, metaModel.createTypedLiteral(harvestDate))

            turtleService.saveInformationModel(
                fdkId = modelMeta.fdkId,
                turtle = metaModel.union(harvested).createRDFResponse(Lang.TURTLE),
                withRecords = true
            )

            return modelMeta
        } else return null
    }

    private fun CatalogAndInfoModels.mapToCatalogMeta(
        harvestDate: Calendar,
        dbMeta: CatalogMeta?
    ): CatalogMeta {
        val catalogURI = resourceURI
        val fdkId = dbMeta?.fdkId ?: createIdFromUri(catalogURI)
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
        val fdkId = dbMeta?.fdkId ?: createIdFromUri(resourceURI)
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

    private fun formatNowWithOsloTimeZone(): String =
        ZonedDateTime.now(ZoneId.of("Europe/Oslo"))
            .format(DateTimeFormatter.ofPattern(dateFormat))

    private fun Calendar.formatWithOsloTimeZone(): String =
        ZonedDateTime.from(toInstant().atZone(ZoneId.of("Europe/Oslo")))
            .format(DateTimeFormatter.ofPattern(dateFormat))
}
