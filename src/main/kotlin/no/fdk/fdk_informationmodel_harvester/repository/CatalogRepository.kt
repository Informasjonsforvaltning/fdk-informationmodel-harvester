package no.fdk.fdk_informationmodel_harvester.repository

import no.fdk.fdk_informationmodel_harvester.model.CatalogMeta
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface CatalogRepository : MongoRepository<CatalogMeta, String>