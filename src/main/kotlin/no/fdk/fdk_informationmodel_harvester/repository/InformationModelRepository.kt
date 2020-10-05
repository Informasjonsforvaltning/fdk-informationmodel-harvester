package no.fdk.fdk_informationmodel_harvester.repository

import no.fdk.fdk_informationmodel_harvester.model.InformationModelDBO
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface InformationModelRepository : MongoRepository<InformationModelDBO, String> {
    fun findOneByFdkId(fdkId: String): InformationModelDBO?
}