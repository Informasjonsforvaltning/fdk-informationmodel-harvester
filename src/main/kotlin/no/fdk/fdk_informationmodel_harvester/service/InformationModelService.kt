package no.fdk.fdk_informationmodel_harvester.service

import no.fdk.fdk_informationmodel_harvester.rdf.createRDFResponse
import no.fdk.fdk_informationmodel_harvester.rdf.parseRDFResponse
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.springframework.stereotype.Service

@Service
class InformationModelService(private val turtleService: TurtleService) {

    fun getAll(returnType: Lang, includeFDKCatalogRecords: Boolean): String =
        turtleService.findUnionModel(includeFDKCatalogRecords)
            ?.let {
                if (returnType == Lang.TURTLE) it
                else parseRDFResponse(it, Lang.TURTLE, null)?.createRDFResponse(returnType)
        } ?: ModelFactory.createDefaultModel().createRDFResponse(returnType)

    fun getInformationModelById(id: String, returnType: Lang, includeFDKCatalogRecords: Boolean): String? =
        turtleService.findInformationModel(id, includeFDKCatalogRecords)
            ?.let {
                if (returnType == Lang.TURTLE) it
                else parseRDFResponse(it, Lang.TURTLE, null)?.createRDFResponse(returnType)
            }

    fun getCatalogById(id: String, returnType: Lang, includeFDKCatalogRecords: Boolean): String? =
        turtleService.findCatalog(id, includeFDKCatalogRecords)
            ?.let {
                if (returnType == Lang.TURTLE) it
                else parseRDFResponse(it, Lang.TURTLE, null)?.createRDFResponse(returnType)
            }

}
