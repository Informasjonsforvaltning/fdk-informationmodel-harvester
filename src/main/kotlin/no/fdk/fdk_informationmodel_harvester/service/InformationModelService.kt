package no.fdk.fdk_informationmodel_harvester.service

import no.fdk.fdk_informationmodel_harvester.model.NO_FDK_UNION_ID
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

    fun getAll(returnType: JenaType, includeFDKCatalogRecords: Boolean): String {
        val id = if (includeFDKCatalogRecords) UNION_ID else NO_FDK_UNION_ID

        return miscellaneousRepository.findByIdOrNull(id)
            ?.let { ungzip(it.turtle) }
            ?.let {
                if (returnType == JenaType.TURTLE) it
                else parseRDFResponse(it, JenaType.TURTLE, null)?.createRDFResponse(returnType)
            }
            ?: ModelFactory.createDefaultModel().createRDFResponse(returnType)
    }

    fun getInformationModelById(id: String, returnType: JenaType, includeFDKCatalogRecords: Boolean): String? =
        infoRepository.findOneByFdkId(id)
            ?.let {
                if (includeFDKCatalogRecords) ungzip(it.turtleInformationModel)
                else ungzip(it.turtleHarvested)
            } ?.let {
                if (returnType == JenaType.TURTLE) it
                else parseRDFResponse(it, JenaType.TURTLE, null)?.createRDFResponse(returnType)
            }

    fun getCatalogById(id: String, returnType: JenaType, includeFDKCatalogRecords: Boolean): String? =
        catalogRepository.findOneByFdkId(id)
            ?.let {
                if (includeFDKCatalogRecords) ungzip(it.turtleCatalog)
                else ungzip(it.turtleHarvested)
            } ?.let {
                if (returnType == JenaType.TURTLE) it
                else parseRDFResponse(it, JenaType.TURTLE, null)?.createRDFResponse(returnType)
            }

}
