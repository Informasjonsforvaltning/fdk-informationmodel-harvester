package no.fdk.fdk_informationmodel_harvester.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "informationModelMeta")
data class InformationModelMeta (
    @Id
    val uri: String,

    @Indexed(unique = true)
    val fdkId: String,

    val isPartOf: String,
    val issued: Long,
    val modified: Long
)

@Document(collection = "catalogMeta")
data class CatalogMeta (
        @Id
        val uri: String,

        @Indexed(unique = true)
        val fdkId: String,

        val issued: Long,
        val modified: Long
)
