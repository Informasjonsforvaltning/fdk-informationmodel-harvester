package no.fdk.fdk_informationmodel_harvester.service

import no.fdk.fdk_informationmodel_harvester.adapter.FusekiAdapter
import no.fdk.fdk_informationmodel_harvester.configuration.ApplicationProperties
import no.fdk.fdk_informationmodel_harvester.harvester.calendarFromTimestamp
import no.fdk.fdk_informationmodel_harvester.model.*
import no.fdk.fdk_informationmodel_harvester.rdf.JenaType
import no.fdk.fdk_informationmodel_harvester.rdf.createRDFResponse
import no.fdk.fdk_informationmodel_harvester.rdf.parseRDFResponse
import no.fdk.fdk_informationmodel_harvester.repository.*
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
    private val turtleService: TurtleService
) {

    fun updateUnionModel() {
        var unionWithRecords = ModelFactory.createDefaultModel()
        var unionNoRecords = ModelFactory.createDefaultModel()

        catalogRepository.findAll()
            .forEach {
                turtleService.findCatalog(it.fdkId, withRecords = true)
                    ?.let { turtle -> parseRDFResponse(turtle, JenaType.TURTLE, null) }
                    ?.run { unionWithRecords = unionWithRecords.union(this) }

                turtleService.findCatalog(it.fdkId, withRecords = false)
                    ?.let { turtle -> parseRDFResponse(turtle, JenaType.TURTLE, null) }
                    ?.run { unionNoRecords = unionNoRecords.union(this) }
            }

        fusekiAdapter.storeUnionModel(unionWithRecords)

        turtleService.saveUnionModel(unionWithRecords.createRDFResponse(JenaType.TURTLE), withRecords = true)
        turtleService.saveUnionModel(unionNoRecords.createRDFResponse(JenaType.TURTLE), withRecords = false)
    }

    fun updateMetaData() {
        catalogRepository.findAll()
            .forEach { catalog ->
                val fdkURI = "${applicationProperties.catalogUri}/${catalog.fdkId}"
                var catalogMeta = catalog.createMetaModel()

                infoRepository.findAllByIsPartOf(fdkURI)
                    .forEach { infoModel ->
                        val infoModelMeta = infoModel.createMetaModel()
                        catalogMeta = catalogMeta.union(infoModelMeta)

                        turtleService.findInformationModel(infoModel.fdkId, withRecords = false)
                            ?.let { infoNoRecords -> parseRDFResponse(infoNoRecords, JenaType.TURTLE, null) }
                            ?.let { infoModelNoRecords -> infoModelMeta.union(infoModelNoRecords).createRDFResponse(JenaType.TURTLE) }
                            ?.run { turtleService.saveInformationModel(fdkId = infoModel.fdkId, turtle = this, withRecords = true) }
                    }

                turtleService.findCatalog(catalog.fdkId, withRecords = false)
                    ?.let { catalogNoRecords -> parseRDFResponse(catalogNoRecords, JenaType.TURTLE, null) }
                    ?.let { catalogModelNoRecords -> catalogMeta.union(catalogModelNoRecords).createRDFResponse(JenaType.TURTLE) }
                    ?.run { turtleService.saveCatalog(fdkId = catalog.fdkId, turtle = this, withRecords = true) }
            }

        updateUnionModel()
    }

    private fun CatalogMeta.createMetaModel(): Model {
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

    private fun InformationModelMeta.createMetaModel(): Model {
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