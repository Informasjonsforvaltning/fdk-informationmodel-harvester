package no.fdk.fdk_informationmodel_harvester.model

import no.fdk.fdk_informationmodel_harvester.rdf.JenaType
import no.fdk.fdk_informationmodel_harvester.rdf.parseRDFResponse
import no.fdk.fdk_informationmodel_harvester.service.ungzip
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document


const val UNION_ID = "information-model-catalogs-union-graph"
const val NO_FDK_UNION_ID = "information-model-catalogs-union-graph-exclude-fdk-catalog-records"

@Document(collection = "informationmodel")
data class InformationModelDBO (
    @Id
    val uri: String,

    @Indexed(unique = true)
    val fdkId: String,

    val isPartOf: String,
    val issued: Long,
    val modified: Long,

    val turtleHarvested: String,
    val turtleInformationModel: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InformationModelDBO

        return when {
            uri != other.uri -> false
            fdkId != other.fdkId -> false
            isPartOf != other.isPartOf -> false
            issued != other.issued -> false
            modified != other.modified -> false
            !zippedModelsAreIsometric(turtleHarvested, other.turtleHarvested) -> false
            else -> zippedModelsAreIsometric(turtleInformationModel, other.turtleInformationModel)
        }
    }

    override fun hashCode(): Int {
        var result = uri.hashCode()
        result = 31 * result + fdkId.hashCode()
        result = 31 * result + isPartOf.hashCode()
        result = 31 * result + issued.hashCode()
        result = 31 * result + modified.hashCode()
        result = 31 * result + turtleHarvested.hashCode()
        result = 31 * result + turtleInformationModel.hashCode()
        return result
    }
}

@Document(collection = "catalog")
data class CatalogDBO (
        @Id
        val uri: String,

        @Indexed(unique = true)
        val fdkId: String,

        val issued: Long,
        val modified: Long,

        val turtleHarvested: String,
        val turtleCatalog: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CatalogDBO

        return when {
            uri != other.uri -> false
            fdkId != other.fdkId -> false
            issued != other.issued -> false
            modified != other.modified -> false
            !zippedModelsAreIsometric(turtleHarvested, other.turtleHarvested) -> false
            else -> zippedModelsAreIsometric(turtleCatalog, other.turtleCatalog)
        }
    }

    override fun hashCode(): Int {
        var result = uri.hashCode()
        result = 31 * result + fdkId.hashCode()
        result = 31 * result + issued.hashCode()
        result = 31 * result + modified.hashCode()
        result = 31 * result + turtleHarvested.hashCode()
        result = 31 * result + turtleCatalog.hashCode()
        return result
    }
}

@Document(collection = "misc")
data class MiscellaneousTurtle (
        @Id val id: String,
        val isHarvestedSource: Boolean,
        val turtle: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MiscellaneousTurtle

        return when {
            id != other.id -> false
            isHarvestedSource != other.isHarvestedSource -> false
            else -> zippedModelsAreIsometric(turtle, other.turtle)
        }

    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + isHarvestedSource.hashCode()
        result = 31 * result + turtle.hashCode()
        return result
    }
}

private fun zippedModelsAreIsometric(zip0: String, zip1: String): Boolean {
    val model0 = parseRDFResponse(ungzip(zip0), JenaType.TURTLE, null)
    val model1 = parseRDFResponse(ungzip(zip1), JenaType.TURTLE, null)

    return when {
        model0 != null && model1 != null -> model0.isIsomorphicWith(model1)
        model0 == null && model1 == null -> true
        else -> false
    }
}
