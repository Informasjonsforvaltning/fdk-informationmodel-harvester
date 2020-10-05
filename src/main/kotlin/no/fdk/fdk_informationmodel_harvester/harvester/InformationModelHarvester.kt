package no.fdk.fdk_informationmodel_harvester.harvester

import no.fdk.fdk_informationmodel_harvester.adapter.FusekiAdapter
import no.fdk.fdk_informationmodel_harvester.configuration.ApplicationProperties
import no.fdk.fdk_informationmodel_harvester.adapter.InformationModelAdapter
import no.fdk.fdk_informationmodel_harvester.repository.CatalogRepository
import no.fdk.fdk_informationmodel_harvester.repository.InformationModelRepository
import no.fdk.fdk_informationmodel_harvester.repository.MiscellaneousRepository
import no.fdk.fdk_informationmodel_harvester.service.gzip
import no.fdk.fdk_informationmodel_harvester.service.ungzip
import no.fdk.fdk_informationmodel_harvester.model.*
import no.fdk.fdk_informationmodel_harvester.rdf.*
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.sparql.vocabulary.FOAF
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*

private val LOGGER = LoggerFactory.getLogger(InformationModelHarvester::class.java)

@Service
class InformationModelHarvester(
    private val adapter: InformationModelAdapter,
    private val fusekiAdapter: FusekiAdapter,
    private val catalogRepository: CatalogRepository,
    private val informationModelRepository: InformationModelRepository,
    private val miscRepository: MiscellaneousRepository,
    private val applicationProperties: ApplicationProperties
) {

    fun updateUnionModel() {
        var unionModel = ModelFactory.createDefaultModel()

        catalogRepository.findAll()
            .map { parseRDFResponse(ungzip(it.turtleCatalog), JenaType.TURTLE, null) }
            .forEach { unionModel = unionModel.union(it) }

        fusekiAdapter.storeUnionModel(unionModel)

        miscRepository.save(
            MiscellaneousTurtle(
                id = UNION_ID,
                isHarvestedSource = false,
                turtle = gzip(unionModel.createRDFResponse(JenaType.TURTLE))
            )
        )
    }

    fun harvestInformationModelCatalog(source: HarvestDataSource, harvestDate: Calendar) =
        if (source.url != null) {
            LOGGER.debug("Starting harvest of ${source.url}")
            val jenaWriterType = jenaTypeFromAcceptHeader(source.acceptHeaderValue)

            val harvested = when (jenaWriterType) {
                null -> null
                JenaType.NOT_JENA -> null
                else -> adapter.getInformationModels(source)?.let { parseRDFResponse(it, jenaWriterType, source.url) }
            }

            when {
                jenaWriterType == null -> LOGGER.error("Not able to harvest from ${source.url}, no accept header supplied")
                jenaWriterType == JenaType.NOT_JENA -> LOGGER.error("Not able to harvest from ${source.url}, header ${source.acceptHeaderValue} is not acceptable ")
                harvested == null -> LOGGER.info("Not able to harvest ${source.url}")
                else -> checkHarvestedContainsChanges(harvested, source.url, harvestDate)
            }
        } else LOGGER.error("Harvest source is not defined")

    private fun checkHarvestedContainsChanges(harvested: Model, sourceURL: String, harvestDate: Calendar) {
        val dbId = createIdFromUri(sourceURL)
        val dbData = miscRepository
            .findByIdOrNull(sourceURL)
            ?.let { parseRDFResponse(ungzip(it.turtle), JenaType.TURTLE, null) }

        if (dbData != null && harvested.isIsomorphicWith(dbData)) {
            LOGGER.info("No changes from last harvest of $sourceURL")
        } else {
            LOGGER.info("Changes detected, saving data from $sourceURL on graph $dbId, and updating FDK meta data")
            miscRepository.save(
                MiscellaneousTurtle(
                    id = sourceURL,
                    isHarvestedSource = true,
                    turtle = gzip(harvested.createRDFResponse(JenaType.TURTLE))
                )
            )

            val catalogs = splitCatalogsFromRDF(harvested)

            if (catalogs.isEmpty()) LOGGER.error("No catalog with information models found in data harvested from $sourceURL")
            else updateDB(catalogs, harvestDate)
        }
    }

    private fun updateDB(catalogs: List<CatalogAndInfoModels>, harvestDate: Calendar) {
        val catalogsToSave = mutableListOf<CatalogDBO>()
        val modelsToSave = mutableListOf<InformationModelDBO>()

        catalogs
            .map { Pair(it, catalogRepository.findByIdOrNull(it.resource.uri)) }
            .filter { it.first.harvestDiff(it.second) }
            .forEach {
                val catalogURI = it.first.resource.uri

                val fdkId = it.second?.fdkId ?: createIdFromUri(catalogURI)
                val fdkUri = "${applicationProperties.catalogUri}/$fdkId"

                val issued = it.second?.issued
                    ?.let { timestamp -> calendarFromTimestamp(timestamp) }
                    ?: harvestDate

                var catalogModel = it.first.harvestedCatalogWithoutInfoModels

                catalogModel.createResource(fdkUri)
                    .addProperty(RDF.type, DCAT.CatalogRecord)
                    .addProperty(DCTerms.identifier, fdkId)
                    .addProperty(FOAF.primaryTopic, catalogModel.createResource(catalogURI))
                    .addProperty(DCTerms.issued, catalogModel.createTypedLiteral(issued))
                    .addProperty(DCTerms.modified, catalogModel.createTypedLiteral(harvestDate))

                val modelsWithChanges = it.first.models
                    .map { infoModel ->
                        val dbInfoModel = informationModelRepository.findByIdOrNull(infoModel.resource.uri)
                        if (dbInfoModel == null || infoModel.harvestDiff(dbInfoModel)) {
                            Pair(infoModel.mapToUpdatedDBO(harvestDate, fdkUri, dbInfoModel), true)
                        } else {
                            Pair(dbInfoModel, false)
                        }
                    }

                modelsWithChanges
                    .map { pair -> pair.first }
                    .map { dataset -> parseRDFResponse(ungzip(dataset.turtleInformationModel), JenaType.TURTLE, null) }
                    .forEach { model -> catalogModel = catalogModel.union(model) }

                modelsWithChanges
                    .filter { dsWithChanged -> dsWithChanged.second }
                    .forEach { pair -> modelsToSave.add(pair.first) }

                catalogsToSave.add(
                    CatalogDBO(
                        uri = catalogURI,
                        fdkId = fdkId,
                        issued = issued.timeInMillis,
                        modified = harvestDate.timeInMillis,
                        turtleHarvested = gzip(it.first.harvestedCatalog.createRDFResponse(JenaType.TURTLE)),
                        turtleCatalog = gzip(catalogModel.createRDFResponse(JenaType.TURTLE))
                    )
                )
            }

        catalogRepository.saveAll(catalogsToSave)
        informationModelRepository.saveAll(modelsToSave)
    }

    private fun InformationModelRDFModel.mapToUpdatedDBO(
        harvestDate: Calendar,
        catalogURI: String,
        dbService: InformationModelDBO?
    ): InformationModelDBO {
        val fdkId = dbService?.fdkId ?: createIdFromUri(resource.uri)
        val fdkUri = "${applicationProperties.informationModelUri}/$fdkId"

        val metaModel = ModelFactory.createDefaultModel()

        val issued: Calendar = dbService?.issued
            ?.let { timestamp -> calendarFromTimestamp(timestamp) }
            ?: harvestDate

        metaModel.createResource(fdkUri)
            .addProperty(RDF.type, DCAT.CatalogRecord)
            .addProperty(DCTerms.identifier, fdkId)
            .addProperty(FOAF.primaryTopic, metaModel.createResource(resource.uri))
            .addProperty(DCTerms.isPartOf, metaModel.createResource(catalogURI))
            .addProperty(DCTerms.issued, metaModel.createTypedLiteral(issued))
            .addProperty(DCTerms.modified, metaModel.createTypedLiteral(harvestDate))

        return InformationModelDBO(
            uri = resource.uri,
            fdkId = fdkId,
            isPartOf = catalogURI,
            issued = issued.timeInMillis,
            modified = harvestDate.timeInMillis,
            turtleHarvested = gzip(harvested.createRDFResponse(JenaType.TURTLE)),
            turtleInformationModel = gzip(metaModel.union(harvested).createRDFResponse(JenaType.TURTLE))
        )
    }
}