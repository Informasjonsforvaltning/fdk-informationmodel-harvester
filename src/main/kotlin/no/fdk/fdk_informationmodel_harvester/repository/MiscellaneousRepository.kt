package no.fdk.fdk_informationmodel_harvester.repository

import no.fdk.fdk_informationmodel_harvester.model.MiscellaneousTurtle
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface MiscellaneousRepository : MongoRepository<MiscellaneousTurtle, String>