package no.fdk.fdk_informationmodel_harvester.service

import no.fdk.fdk_informationmodel_harvester.configuration.ApplicationProperties
import no.fdk.fdk_informationmodel_harvester.harvester.calendarFromTimestamp
import no.fdk.fdk_informationmodel_harvester.harvester.extractCatalogModel
import no.fdk.fdk_informationmodel_harvester.model.*
import no.fdk.fdk_informationmodel_harvester.rdf.*
import no.fdk.fdk_informationmodel_harvester.repository.*
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.apache.jena.sparql.vocabulary.FOAF
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF
import org.springframework.stereotype.Service

@Service
class UpdateService(
    private val applicationProperties: ApplicationProperties,
    private val catalogRepository: CatalogRepository,
    private val infoRepository: InformationModelRepository,
    private val turtleService: TurtleService
) {

    fun updateMetaData() {
        catalogRepository.findAll()
            .forEach { catalog ->
                val fdkURI = "${applicationProperties.catalogUri}/${catalog.fdkId}"
                val catalogMeta = catalog.createMetaModel()
                val completeMetaModel = ModelFactory.createDefaultModel()
                completeMetaModel.add(catalogMeta)
                turtleService.findCatalog(catalog.fdkId, withRecords = false)
                    ?.let { catalogTurtle -> safeParseRDF(catalogTurtle, Lang.TURTLE) }
                    ?.let { catalogNoRecords ->
                        val catalogTriples = catalogNoRecords.getResource(catalog.uri)
                            .extractCatalogModel()
                        catalogTriples.add(catalogMeta)

                        infoRepository.findAllByIsPartOf(fdkURI)
                            .filter { catalogContainsInfoModel(catalogNoRecords, catalog.uri, it.uri) }
                            .forEach { infoModel ->
                                val infoModelMeta = infoModel.createMetaModel()
                                completeMetaModel.add(infoModelMeta)

                                turtleService.findInformationModel(infoModel.fdkId, withRecords = false)
                                    ?.let { infoNoRecords -> safeParseRDF(infoNoRecords, Lang.TURTLE) }
                                    ?.let { infoModelNoRecords ->
                                        infoModelMeta.union(infoModelNoRecords)
                                            .union(catalogTriples)
                                            .createRDFResponse(Lang.TURTLE)
                                    }
                                    ?.run {
                                        turtleService.saveInformationModel(
                                            fdkId = infoModel.fdkId,
                                            turtle = this,
                                            withRecords = true
                                        )
                                    }
                            }

                        completeMetaModel.union(catalogNoRecords).createRDFResponse(Lang.TURTLE)
                            .run { turtleService.saveCatalog(fdkId = catalog.fdkId, turtle = this, withRecords = true) }
                    }
            }
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
