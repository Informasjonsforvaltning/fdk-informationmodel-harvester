package no.fdk.fdk_informationmodel_harvester.service

import no.fdk.fdk_informationmodel_harvester.model.UNION_ID
import no.fdk.fdk_informationmodel_harvester.repository.CatalogRepository
import no.fdk.fdk_informationmodel_harvester.repository.InformationModelRepository
import no.fdk.fdk_informationmodel_harvester.repository.MiscellaneousRepository
import no.fdk.fdk_informationmodel_harvester.rdf.JenaType
import no.fdk.fdk_informationmodel_harvester.rdf.createRDFResponse
import no.fdk.fdk_informationmodel_harvester.rdf.parseRDFResponse
import org.apache.jena.rdf.model.ModelFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class InformationModelService(
    private val catalogRepository: CatalogRepository,
    private val infoRepository: InformationModelRepository,
    private val miscellaneousRepository: MiscellaneousRepository
) {

    fun countMetaData(): Long =
        catalogRepository.count()

    fun getAll(returnType: JenaType): String =
        miscellaneousRepository.findByIdOrNull(UNION_ID)
            ?.let { ungzip(it.turtle) }
            ?.let {
                if (returnType == JenaType.TURTLE) it
                else parseRDFResponse(it, JenaType.TURTLE, null)?.createRDFResponse(returnType)
            }
            ?: ModelFactory.createDefaultModel().createRDFResponse(returnType)

    fun getInformationModelById(id: String, returnType: JenaType): String? =
        infoRepository.findOneByFdkId(id)
            ?.let { ungzip(it.turtleInformationModel) }
            ?.let {
                if (returnType == JenaType.TURTLE) it
                else parseRDFResponse(it, JenaType.TURTLE, null)?.createRDFResponse(returnType)
            }

    fun getCatalogById(id: String, returnType: JenaType): String? =
        catalogRepository.findOneByFdkId(id)
            ?.let { ungzip(it.turtleCatalog) }
            ?.let {
                if (returnType == JenaType.TURTLE) it
                else parseRDFResponse(it, JenaType.TURTLE, null)?.createRDFResponse(returnType)
            }

}
