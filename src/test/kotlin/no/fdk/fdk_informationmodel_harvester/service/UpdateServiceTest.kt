package no.fdk.fdk_informationmodel_harvester.service

import no.fdk.fdk_informationmodel_harvester.configuration.ApplicationProperties
import no.fdk.fdk_informationmodel_harvester.rdf.parseRDFResponse
import no.fdk.fdk_informationmodel_harvester.repository.CatalogRepository
import no.fdk.fdk_informationmodel_harvester.repository.InformationModelRepository
import no.fdk.fdk_informationmodel_harvester.utils.*
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@Tag("unit")
class UpdateServiceTest {
    private val catalogRepository: CatalogRepository = mock()
    private val modelRepository: InformationModelRepository = mock()
    private val valuesMock: ApplicationProperties = mock()
    private val turtleService: TurtleService = mock()
    private val updateService = UpdateService(
        valuesMock, catalogRepository, modelRepository, turtleService)

    private val responseReader = TestResponseReader()

    @Nested
    internal inner class UpdateMetaData {

        @Test
        fun catalogRecordsIsRecreatedFromMetaDBO() {
            whenever(catalogRepository.findAll())
                .thenReturn(listOf(CATALOG_DBO_0, CATALOG_DBO_1))
            whenever(modelRepository.findAllByIsPartOf("http://localhost:5050/catalogs/${CATALOG_DBO_0.fdkId}"))
                .thenReturn(listOf(INFO_MODEL_DBO_0))
            whenever(modelRepository.findAllByIsPartOf("http://localhost:5050/catalogs/${CATALOG_DBO_1.fdkId}"))
                .thenReturn(listOf(INFO_MODEL_DBO_1))
            whenever(turtleService.findCatalog(CATALOG_ID_0, false))
                .thenReturn(responseReader.readFile("no_meta_catalog_0.ttl"))
            whenever(turtleService.findCatalog(CATALOG_ID_1, false))
                .thenReturn(responseReader.readFile("harvest_response_1.ttl"))
            whenever(turtleService.findInformationModel(INFO_MODEL_ID_0, false))
                .thenReturn(responseReader.readFile("no_meta_model_0.ttl"))
            whenever(turtleService.findInformationModel(INFO_MODEL_ID_1, false))
                .thenReturn(responseReader.readFile("no_meta_model_1.ttl"))


            whenever(valuesMock.catalogUri)
                .thenReturn("http://localhost:5050/catalogs")
            whenever(valuesMock.informationModelUri)
                .thenReturn("http://localhost:5050/informationmodels")

            updateService.updateMetaData()

            val expectedCatalog0 = responseReader.parseFile("catalog_0.ttl", "TURTLE")
            val expectedInfoModel0 = responseReader.parseFile("model_0.ttl", "TURTLE")
            val expectedCatalog1 = responseReader.parseFile("catalog_1.ttl", "TURTLE")
            val expectedInfoModel1 = responseReader.parseFile("model_1.ttl", "TURTLE")

            argumentCaptor<String, String, Boolean>().apply {
                verify(turtleService, times(2)).saveCatalog(first.capture(), second.capture(), third.capture())
                assertEquals(listOf(CATALOG_ID_0, CATALOG_ID_1), first.allValues)
                assertTrue(checkIfIsomorphicAndPrintDiff(parseRDFResponse(second.firstValue, Lang.TURTLE), expectedCatalog0, "diffInMetaDataUpdatesTurtle-catalog0"))
                assertTrue(checkIfIsomorphicAndPrintDiff(parseRDFResponse(second.secondValue, Lang.TURTLE), expectedCatalog1, "diffInMetaDataUpdatesTurtle-catalog1"))
                assertEquals(listOf(true, true), third.allValues)
            }

            argumentCaptor<String, String, Boolean>().apply {
                verify(turtleService, times(2)).saveInformationModel(first.capture(), second.capture(), third.capture())
                assertEquals(listOf(INFO_MODEL_ID_0, INFO_MODEL_ID_1), first.allValues)
                assertTrue(checkIfIsomorphicAndPrintDiff(parseRDFResponse(second.firstValue, Lang.TURTLE), expectedInfoModel0, "diffInMetaDataUpdatesTurtle-model0"))
                assertTrue(checkIfIsomorphicAndPrintDiff(parseRDFResponse(second.secondValue, Lang.TURTLE), expectedInfoModel1, "diffInMetaDataUpdatesTurtle-model1"))
                assertEquals(listOf(true, true), third.allValues)
            }
        }



        @Test
        fun updateIsSkippedIfNotActuallyPresentInCatalog() {
            whenever(catalogRepository.findAll())
                .thenReturn(listOf(CATALOG_DBO_0))
            whenever(modelRepository.findAllByIsPartOf("http://localhost:5050/catalogs/${CATALOG_ID_0}"))
                .thenReturn(listOf(INFO_MODEL_DBO_0, INFO_MODEL_DBO_1))
            whenever(turtleService.findCatalog(CATALOG_ID_0, false))
                .thenReturn(responseReader.readFile("harvest_response_0_no_models.ttl"))
            whenever(turtleService.findInformationModel(INFO_MODEL_ID_0, false))
                .thenReturn(responseReader.readFile("no_meta_model_0.ttl"))
            whenever(turtleService.findInformationModel(INFO_MODEL_ID_1, false))
                .thenReturn(responseReader.readFile("no_meta_model_1.ttl"))

            whenever(valuesMock.catalogUri)
                .thenReturn("http://localhost:5050/catalogs")
            whenever(valuesMock.informationModelUri)
                .thenReturn("http://localhost:5050/informationmodels")

            updateService.updateMetaData()

            val expectedCatalog = responseReader.parseFile("catalog_0_no_models.ttl", "TURTLE")

            argumentCaptor<String, String, Boolean>().apply {
                verify(turtleService, times(1)).saveCatalog(first.capture(), second.capture(), third.capture())
                assertEquals(CATALOG_ID_0, first.firstValue)
                assertTrue(checkIfIsomorphicAndPrintDiff(parseRDFResponse(second.firstValue, Lang.TURTLE), expectedCatalog, "updateIsSkippedIfNotActuallyPresentInCatalog"))
                assertEquals(listOf(true), third.allValues)
            }

            argumentCaptor<String, String, Boolean>().apply {
                verify(turtleService, times(0)).saveInformationModel(first.capture(), second.capture(), third.capture())
            }
        }

    }
}