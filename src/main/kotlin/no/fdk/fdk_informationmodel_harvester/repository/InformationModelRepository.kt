package no.fdk.fdk_informationmodel_harvester.repository

import no.fdk.fdk_informationmodel_harvester.model.InformationModelMeta
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface InformationModelRepository : MongoRepository<InformationModelMeta, String> {
    fun findAllByIsPartOf(isPartOf: String): List<InformationModelMeta>
}