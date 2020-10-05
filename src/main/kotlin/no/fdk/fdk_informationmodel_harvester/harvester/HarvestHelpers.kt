package no.fdk.fdk_informationmodel_harvester.harvester

import no.fdk.fdk_informationmodel_harvester.model.CatalogDBO
import no.fdk.fdk_informationmodel_harvester.model.InformationModelDBO
import no.fdk.fdk_informationmodel_harvester.rdf.*
import no.fdk.fdk_informationmodel_harvester.service.ungzip
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.RDF
import java.util.*


fun CatalogAndInfoModels.harvestDiff(dbo: CatalogDBO?): Boolean =
    if (dbo == null) true
    else !harvestedCatalog.isIsomorphicWith(parseRDFResponse(ungzip(dbo.turtleHarvested), JenaType.TURTLE, null))

fun InformationModelRDFModel.harvestDiff(dbo: InformationModelDBO): Boolean =
    !harvested.isIsomorphicWith(parseRDFResponse(ungzip(dbo.turtleHarvested), JenaType.TURTLE, null))

fun splitCatalogsFromRDF(harvested: Model): List<CatalogAndInfoModels> =
    harvested.listResourcesWithProperty(RDF.type, DCAT.Catalog)
        .toList()
        .filter { it.hasProperty(ModellDCATAPNO.model) }
        .map { catalogResource ->
            val catalogInfoModels: List<InformationModelRDFModel> = catalogResource.listProperties(ModellDCATAPNO.model)
                .toList()
                .map { dataset -> dataset.resource.extractInformationModel() }

            var catalogModelWithoutInfoModels = catalogResource.listProperties().toModel()
            catalogModelWithoutInfoModels.setNsPrefixes(harvested.nsPrefixMap)

            catalogResource.listProperties().toList()
                .filter { it.isResourceProperty() }
                .forEach {
                    if (it.predicate != ModellDCATAPNO.model) {
                        catalogModelWithoutInfoModels = catalogModelWithoutInfoModels.recursiveAddNonInformationModelResource(it.resource, 5)
                    }
                }

            var catalogModel = catalogModelWithoutInfoModels
            catalogInfoModels.forEach { catalogModel = catalogModel.union(it.harvested) }

            CatalogAndInfoModels(
                resource = catalogResource,
                harvestedCatalog = catalogModel,
                harvestedCatalogWithoutInfoModels = catalogModelWithoutInfoModels,
                models = catalogInfoModels
            )
        }

fun Resource.extractInformationModel(): InformationModelRDFModel {
    var infoModel = listProperties().toModel()
    infoModel = infoModel.setNsPrefixes(model.nsPrefixMap)

    listProperties().toList()
        .filter { it.isResourceProperty() }
        .forEach { infoModel = infoModel.recursiveAddNonInformationModelResource(it.resource, 10) }

    return InformationModelRDFModel(resource = this, harvested = infoModel)
}

private fun Model.recursiveAddNonInformationModelResource(resource: Resource, recursiveCount: Int): Model {
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
    }

    return this
}

fun calendarFromTimestamp(timestamp: Long): Calendar {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp
    return calendar
}

data class CatalogAndInfoModels (
    val resource: Resource,
    val harvestedCatalog: Model,
    val harvestedCatalogWithoutInfoModels: Model,
    val models: List<InformationModelRDFModel>,
)

data class InformationModelRDFModel (
    val resource: Resource,
    val harvested: Model
)