package no.fdk.fdk_informationmodel_harvester.service

import no.fdk.fdk_informationmodel_harvester.adapter.FusekiAdapter
import no.fdk.fdk_informationmodel_harvester.configuration.ApplicationProperties
import no.fdk.fdk_informationmodel_harvester.harvester.calendarFromTimestamp
import no.fdk.fdk_informationmodel_harvester.model.*
import no.fdk.fdk_informationmodel_harvester.rdf.JenaType
import no.fdk.fdk_informationmodel_harvester.rdf.createRDFResponse
import no.fdk.fdk_informationmodel_harvester.rdf.parseRDFResponse
import no.fdk.fdk_informationmodel_harvester.repository.CatalogRepository
import no.fdk.fdk_informationmodel_harvester.repository.InformationModelRepository
import no.fdk.fdk_informationmodel_harvester.repository.MiscellaneousRepository
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.sparql.vocabulary.FOAF
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF
import org.springframework.stereotype.Service

@Service
class UpdateService (
    private val applicationProperties: ApplicationProperties,
    private val fusekiAdapter: FusekiAdapter,
    private val catalogRepository: CatalogRepository,
    private val infoRepository: InformationModelRepository,
    private val miscRepository: MiscellaneousRepository
) {

    fun updateUnionModel() {
        var unionModel = ModelFactory.createDefaultModel()
        var unionModelExcludedFDKMetaData = ModelFactory.createDefaultModel()

        catalogRepository.findAll()
            .forEach {
                val metaModel = parseRDFResponse(ungzip(it.turtleCatalog), JenaType.TURTLE, null)
                val noMetaModel = parseRDFResponse(ungzip(it.turtleHarvested), JenaType.TURTLE, null)

                unionModel = unionModel.union(metaModel)
                unionModelExcludedFDKMetaData = unionModelExcludedFDKMetaData.union(noMetaModel)
            }

        fusekiAdapter.storeUnionModel(unionModel)

        miscRepository.saveAll(
            listOf(
                MiscellaneousTurtle(
                    id = UNION_ID,
                    isHarvestedSource = false,
                    turtle = gzip(unionModel.createRDFResponse(JenaType.TURTLE))
                ),
                MiscellaneousTurtle(
                    id = NO_FDK_UNION_ID,
                    isHarvestedSource = false,
                    turtle = gzip(unionModelExcludedFDKMetaData.createRDFResponse(JenaType.TURTLE))
                )
            )
        )
    }

    fun updateMetaData() {
        val catalogsToSave = mutableListOf<CatalogDBO>()
        val infoModelsToSave = mutableListOf<InformationModelDBO>()

        catalogRepository.findAll()
            .forEach { catalog ->
                val fdkURI = "${applicationProperties.catalogUri}/${catalog.fdkId}"
                var catalogMeta = catalog.createMetaModel()

                infoRepository.findAllByIsPartOf(fdkURI)
                    .forEach { infoModel ->
                        val infoModelMeta = infoModel.createMetaModel()
                        catalogMeta = catalogMeta.union(infoModelMeta)

                        val infoModelHarvested = parseRDFResponse(ungzip(infoModel.turtleHarvested), JenaType.TURTLE, null)
                        val infoModelTurtle = infoModelMeta.union(infoModelHarvested).createRDFResponse(JenaType.TURTLE)

                        infoModelsToSave.add(infoModel.copy(turtleInformationModel = gzip(infoModelTurtle)))
                    }

                val catalogHarvested = parseRDFResponse(ungzip(catalog.turtleHarvested), JenaType.TURTLE, null)
                val catalogTurtle = catalogMeta.union(catalogHarvested).createRDFResponse(JenaType.TURTLE)

                catalogsToSave.add(catalog.copy(turtleCatalog = gzip(catalogTurtle)))
            }

        infoRepository.saveAll(infoModelsToSave)
        catalogRepository.saveAll(catalogsToSave)
        updateUnionModel()
    }

    private fun CatalogDBO.createMetaModel(): Model {
        val fdkUri = "${applicationProperties.catalogUri}/$fdkId"

        val metaModel = ModelFactory.createDefaultModel()

        metaModel.createResource(fdkUri)
            .addProperty(RDF.type, DCAT.CatalogRecord)
            .addProperty(DCTerms.identifier, fdkId)
            .addProperty(FOAF.primaryTopic, metaModel.createResource(uri))
            .addProperty(DCTerms.issued, metaModel.createTypedLiteral(calendarFromTimestamp(issued)))
            .addProperty(DCTerms.modified, metaModel.createTypedLiteral(calendarFromTimestamp(modified)))

        return metaModel
    }

    private fun InformationModelDBO.createMetaModel(): Model {
        val fdkUri = "${applicationProperties.informationModelUri}/$fdkId"

        val metaModel = ModelFactory.createDefaultModel()

        metaModel.createResource(fdkUri)
            .addProperty(RDF.type, DCAT.CatalogRecord)
            .addProperty(DCTerms.identifier, fdkId)
            .addProperty(FOAF.primaryTopic, metaModel.createResource(uri))
            .addProperty(DCTerms.isPartOf, metaModel.createResource(isPartOf))
            .addProperty(DCTerms.issued, metaModel.createTypedLiteral(calendarFromTimestamp(issued)))
            .addProperty(DCTerms.modified, metaModel.createTypedLiteral(calendarFromTimestamp(modified)))

        return metaModel
    }
}