package no.fdk.fdk_informationmodel_harvester.harvester

import no.fdk.fdk_informationmodel_harvester.Application
import no.fdk.fdk_informationmodel_harvester.rdf.*
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement
import org.apache.jena.riot.Lang
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.SKOS
import org.slf4j.LoggerFactory
import java.util.*

private val LOGGER = LoggerFactory.getLogger(Application::class.java)

fun CatalogAndInfoModels.harvestDiff(dboNoRecords: String?): Boolean =
    if (dboNoRecords == null) true
    else !harvestedCatalog.isIsomorphicWith(parseRDFResponse(dboNoRecords, Lang.TURTLE, null))

fun InformationModelRDFModel.harvestDiff(dboNoRecords: String?): Boolean =
    if (dboNoRecords == null) true
    else !harvested.isIsomorphicWith(parseRDFResponse(dboNoRecords, Lang.TURTLE, null))

fun splitCatalogsFromRDF(harvested: Model, sourceURL: String): List<CatalogAndInfoModels> =
    harvested.listResourcesWithProperty(RDF.type, DCAT.Catalog)
        .toList()
        .filterBlankNodeCatalogsAndModels(sourceURL)
        .filter { it.hasProperty(ModellDCATAPNO.model) }
        .map { catalogResource ->
            val catalogInfoModels: List<InformationModelRDFModel> = catalogResource.listProperties(ModellDCATAPNO.model)
                .toList()
                .map { it.resource }
                .filterBlankNodeCatalogsAndModels(sourceURL)
                .filter { catalogContainsInfoModel(harvested, catalogResource.uri, it.uri) }
                .map { infoModel -> infoModel.extractInformationModel() }

            val catalogModelWithoutInfoModels = ModelFactory.createDefaultModel()
            catalogModelWithoutInfoModels.setNsPrefixes(harvested.nsPrefixMap)

            catalogResource.listProperties().toList()
                .forEach { catalogModelWithoutInfoModels.addCatalogProperties(it) }

            var catalogModel = catalogModelWithoutInfoModels
            catalogInfoModels.forEach { catalogModel = catalogModel.union(it.harvested) }

            CatalogAndInfoModels(
                resourceURI = catalogResource.uri,
                harvestedCatalog = catalogModel,
                harvestedCatalogWithoutInfoModels = catalogModelWithoutInfoModels,
                models = catalogInfoModels
            )
        }

private fun List<Resource>.filterBlankNodeCatalogsAndModels(sourceURL: String): List<Resource> =
    filter {
        if (it.isURIResource) true
        else {
            LOGGER.error(
                "Failed harvest of catalog or model for $sourceURL, unable to harvest blank node catalogs and models",
                Exception("unable to harvest blank node catalogs and models")
            )
            false
        }
    }

private fun Model.addCatalogProperties(property: Statement): Model =
    when {
        property.predicate != ModellDCATAPNO.model && property.isResourceProperty() ->
            add(property).recursiveAddNonInformationModelResource(property.resource, 5)
        property.predicate != ModellDCATAPNO.model -> add(property)
        property.isResourceProperty() && property.resource.isURIResource -> add(property)
        else -> this
    }

fun Resource.extractInformationModel(): InformationModelRDFModel {
    var infoModel = listProperties().toModel()
    infoModel = infoModel.setNsPrefixes(model.nsPrefixMap)

    listProperties().toList()
        .filter { it.isResourceProperty() }
        .forEach { infoModel = infoModel.recursiveAddNonInformationModelResource(it.resource, 10) }

    return InformationModelRDFModel(resourceURI = uri, harvested = infoModel)
}

private fun Model.addCodeElementsAssociatedWithCodeList(resource: Resource): Model {
    resource.model
        .listResourcesWithProperty(RDF.type, ModellDCATAPNO.CodeElement)
        .toList()
        .filter { it.hasProperty(SKOS.inScheme, resource) }
        .forEach { codeElement ->
            add(codeElement.listProperties())

            codeElement.listProperties().toList()
                .filter { it.isResourceProperty() }
                .forEach { add(it.resource.listProperties()) }
        }

    return this
}

private fun Model.recursiveAddNonInformationModelResource(resource: Resource, recursiveCount: Int): Model =
    if (resource.isURIResource && containsTriple("<${resource.uri}>", "a", "?o")) this
    else {
        val newCount = recursiveCount - 1
        val types = resource.listProperties(RDF.type)
            .toList()
            .map { it.`object` }

        if (!types.contains(ModellDCATAPNO.InformationModel)) {

            add(resource.listProperties())

            if (newCount > 0) {
                resource.listProperties().toList()
                    .filter { it.isResourceProperty() }
                    .forEach { recursiveAddNonInformationModelResource(it.resource, newCount) }
            }

            if (types.contains(ModellDCATAPNO.CodeList)) addCodeElementsAssociatedWithCodeList(resource)
        }

        this
    }

fun calendarFromTimestamp(timestamp: Long): Calendar {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp
    return calendar
}

data class CatalogAndInfoModels (
    val resourceURI: String,
    val harvestedCatalog: Model,
    val harvestedCatalogWithoutInfoModels: Model,
    val models: List<InformationModelRDFModel>,
)

data class InformationModelRDFModel (
    val resourceURI: String,
    val harvested: Model
)

class HarvestException(url: String) : Exception("Harvest failed for $url")
