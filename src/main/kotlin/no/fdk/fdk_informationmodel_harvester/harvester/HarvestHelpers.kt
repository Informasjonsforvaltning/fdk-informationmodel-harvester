package no.fdk.fdk_informationmodel_harvester.harvester

import no.fdk.fdk_informationmodel_harvester.Application
import no.fdk.fdk_informationmodel_harvester.rdf.*
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement
import org.apache.jena.riot.Lang
import org.apache.jena.util.ResourceUtils
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.SKOS
import org.slf4j.LoggerFactory
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

private val LOGGER = LoggerFactory.getLogger(Application::class.java)
private const val dateFormat: String = "yyyy-MM-dd HH:mm:ss Z"

fun CatalogAndInfoModels.harvestDiff(dboNoRecords: String?): Boolean =
    if (dboNoRecords == null) true
    else !harvestedCatalog.isIsomorphicWith(safeParseRDF(dboNoRecords, Lang.TURTLE))

fun InformationModelRDFModel.harvestDiff(dboNoRecords: String?): Boolean =
    if (dboNoRecords == null) true
    else !harvested.isIsomorphicWith(safeParseRDF(dboNoRecords, Lang.TURTLE))

fun splitCatalogsFromRDF(harvested: Model, sourceURL: String): List<CatalogAndInfoModels> =
    harvested.listResourcesWithProperty(RDF.type, DCAT.Catalog)
        .toList()
        .filterBlankNodeCatalogsAndModels(sourceURL)
        .map { catalogResource ->
            val catalogInfoModels: List<InformationModelRDFModel> = catalogResource.listProperties(ModellDCATAPNO.model)
                .toList()
                .map { it.resource }
                .filterBlankNodeCatalogsAndModels(sourceURL)
                .filter { catalogContainsInfoModel(harvested, catalogResource.uri, it.uri) }
                .map { infoModel -> infoModel.extractInformationModel() }

            val catalogModelWithoutInfoModels = catalogResource.extractCatalogModel()
                .recursiveBlankNodeSkolem(catalogResource.uri)

            val catalogModel = ModelFactory.createDefaultModel()
            catalogInfoModels.forEach { catalogModel.add(it.harvested) }

            CatalogAndInfoModels(
                resourceURI = catalogResource.uri,
                harvestedCatalog = catalogModel.union(catalogModelWithoutInfoModels),
                harvestedCatalogWithoutInfoModels = catalogModelWithoutInfoModels,
                models = catalogInfoModels
            )
        }

fun Resource.extractCatalogModel(): Model {
    val catalogModelWithoutServices = ModelFactory.createDefaultModel()
    catalogModelWithoutServices.setNsPrefixes(model.nsPrefixMap)
    listProperties()
        .toList()
        .forEach { catalogModelWithoutServices.addCatalogProperties(it) }
    return catalogModelWithoutServices
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
            add(property).recursiveAddNonInformationModelResource(property.resource)
        property.predicate != ModellDCATAPNO.model -> add(property)
        property.isResourceProperty() && property.resource.isURIResource -> add(property)
        else -> this
    }

fun Resource.extractInformationModel(): InformationModelRDFModel {
    val infoModel = listProperties().toModel()
    infoModel.setNsPrefixes(model.nsPrefixMap)

    listProperties().toList()
        .filter { it.isResourceProperty() }
        .forEach { infoModel.recursiveAddNonInformationModelResource(it.resource) }

    return InformationModelRDFModel(resourceURI = uri, harvested = infoModel.recursiveBlankNodeSkolem(uri))
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

private fun Model.recursiveAddNonInformationModelResource(resource: Resource): Model =
    if (resource.isURIResource && containsTriple("<${resource.uri}>", "a", "?o")) this
    else {
        val types = resource.listProperties(RDF.type)
            .toList()
            .map { it.`object` }

        if (!types.contains(ModellDCATAPNO.InformationModel)) {
            add(resource.listProperties())

            resource.listProperties().toList()
                .filter { it.isResourceProperty() }
                .forEach { recursiveAddNonInformationModelResource(it.resource) }

            if (types.contains(ModellDCATAPNO.CodeList)) addCodeElementsAssociatedWithCodeList(resource)
        }

        this
    }

private fun Model.recursiveBlankNodeSkolem(baseURI: String): Model {
    val anonSubjects = listSubjects().toList().filter { it.isAnon }
    return if (anonSubjects.isEmpty()) this
    else {
        anonSubjects
            .filter { it.doesNotContainAnon() }
            .forEach {
                ResourceUtils.renameResource(it, "$baseURI/.well-known/skolem/${it.createSkolemID()}")
            }
        this.recursiveBlankNodeSkolem(baseURI)
    }
}

private fun Resource.doesNotContainAnon(): Boolean =
    listProperties().toList()
        .filter { it.isResourceProperty() }
        .map { it.resource }
        .filter { it.listProperties().toList().size > 0 }
        .none { it.isAnon }

private fun Resource.createSkolemID(): String =
    createIdFromString(
        listProperties().toModel()
            .createRDFResponse(Lang.N3)
            .replace("\\s".toRegex(), "")
            .toCharArray()
            .sorted()
            .toString()
    )

fun calendarFromTimestamp(timestamp: Long): Calendar {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp
    return calendar
}

fun formatNowWithOsloTimeZone(): String =
    ZonedDateTime.now(ZoneId.of("Europe/Oslo"))
        .format(DateTimeFormatter.ofPattern(dateFormat))

fun Calendar.formatWithOsloTimeZone(): String =
    ZonedDateTime.from(toInstant().atZone(ZoneId.of("Europe/Oslo")))
        .format(DateTimeFormatter.ofPattern(dateFormat))

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
