package no.fdk.fdk_informationmodel_harvester.harvester

import com.nhaarman.mockitokotlin2.*
import no.fdk.fdk_informationmodel_harvester.adapter.InformationModelAdapter
import no.fdk.fdk_informationmodel_harvester.configuration.ApplicationProperties
import no.fdk.fdk_informationmodel_harvester.model.CatalogMeta
import no.fdk.fdk_informationmodel_harvester.model.InformationModelMeta
import no.fdk.fdk_informationmodel_harvester.rdf.JenaType
import no.fdk.fdk_informationmodel_harvester.rdf.parseRDFResponse
import no.fdk.fdk_informationmodel_harvester.repository.CatalogRepository
import no.fdk.fdk_informationmodel_harvester.repository.InformationModelRepository
import no.fdk.fdk_informationmodel_harvester.service.TurtleService
import no.fdk.fdk_informationmodel_harvester.utils.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.*

@Tag("unit")
class HarvesterTest {
    private val catalogRepository: CatalogRepository = mock()
    private val modelRepository: InformationModelRepository = mock()
    private val turtleService: TurtleService = mock()
    private val valuesMock: ApplicationProperties = mock()
    private val adapter: InformationModelAdapter = mock()

    private val harvester = InformationModelHarvester(
        adapter, catalogRepository,
        modelRepository, turtleService, valuesMock
    )

    private val responseReader = TestResponseReader()

    @Test
    fun harvestDataSourceSavedWhenDBIsEmpty() {
        val harvested = responseReader.readFile("harvest_response_0.ttl")
        val savedInfoModel = responseReader.readFile("model_0.ttl")

        whenever(adapter.getInformationModels(TEST_HARVEST_SOURCE))
            .thenReturn(harvested)
        whenever(modelRepository.findAllByIsPartOf("http://localhost:5000/catalogs/$CATALOG_ID_0"))
            .thenReturn(listOf(INFO_MODEL_DBO_0))
        whenever(turtleService.findInformationModel(INFO_MODEL_ID_0, true))
            .thenReturn(savedInfoModel)

        whenever(valuesMock.catalogUri)
            .thenReturn("http://localhost:5000/catalogs")
        whenever(valuesMock.informationModelUri)
            .thenReturn("http://localhost:5000/informationmodels")

        harvester.harvestInformationModelCatalog(TEST_HARVEST_SOURCE, TEST_HARVEST_DATE)

        val harvestedModel = parseRDFResponse(harvested, JenaType.TURTLE, null)!!

        argumentCaptor<String, String>().apply {
            verify(turtleService, times(1)).saveOne(first.capture(), second.capture())
            assertEquals(TEST_HARVEST_SOURCE.url, first.firstValue)
            Assertions.assertTrue(checkIfIsomorphicAndPrintDiff(parseRDFResponse(second.firstValue, JenaType.TURTLE, null)!!, harvestedModel, "harvestDataSourceSavedWhenDBIsEmpty-harvested"))
        }

        argumentCaptor<CatalogMeta>().apply {
            verify(catalogRepository, times(1)).save(capture())
            assertEquals(CATALOG_DBO_0, firstValue)
        }

        argumentCaptor<InformationModelMeta>().apply {
            verify(modelRepository, times(1)).save(capture())
            assertEquals(INFO_MODEL_DBO_0, firstValue)
        }

        val catalog0 = responseReader.parseFile("catalog_0.ttl", "TURTLE")
        val model0 = parseRDFResponse(savedInfoModel, JenaType.TURTLE, null)!!
        val model0NoMeta = responseReader.parseFile("no_meta_model_0.ttl", "TURTLE")

        argumentCaptor<String, String, Boolean>().apply {
            verify(turtleService, times(2)).saveCatalog(first.capture(), second.capture(), third.capture())
            assertEquals(listOf(CATALOG_ID_0, CATALOG_ID_0), first.allValues)
            Assertions.assertTrue(checkIfIsomorphicAndPrintDiff(parseRDFResponse(second.firstValue, JenaType.TURTLE, null)!!, harvestedModel, "harvestDataSourceSavedWhenDBIsEmpty-no-record-catalog"))
            Assertions.assertTrue(checkIfIsomorphicAndPrintDiff(parseRDFResponse(second.secondValue, JenaType.TURTLE, null)!!, catalog0, "harvestDataSourceSavedWhenDBIsEmpty-record-catalog"))
            assertEquals(listOf(false, true), third.allValues)
        }

        argumentCaptor<String, String, Boolean>().apply {
            verify(turtleService, times(2)).saveInformationModel(first.capture(), second.capture(), third.capture())
            assertEquals(listOf(INFO_MODEL_ID_0, INFO_MODEL_ID_0), first.allValues)
            Assertions.assertTrue(checkIfIsomorphicAndPrintDiff(parseRDFResponse(second.firstValue, JenaType.TURTLE, null)!!, model0NoMeta, "harvestDataSourceSavedWhenDBIsEmpty-record-model"))
            Assertions.assertTrue(checkIfIsomorphicAndPrintDiff(parseRDFResponse(second.secondValue, JenaType.TURTLE, null)!!, model0, "harvestDataSourceSavedWhenDBIsEmpty-no-record-model"))
            assertEquals(listOf(false, true), third.allValues)
        }
    }

    @Test
    fun sourceWithCodeListIsParsedCorrectly() {
        val harvested = responseReader.readFile("harvest_response_2.ttl")
        val savedModel2 = responseReader.readFile("model_2.ttl")
        val savedModel3 = responseReader.readFile("model_3.ttl")

        whenever(adapter.getInformationModels(TEST_HARVEST_SOURCE_2))
            .thenReturn(harvested)
        whenever(modelRepository.findAllByIsPartOf("http://localhost:5000/catalogs/$CATALOG_ID_2"))
            .thenReturn(listOf(INFO_MODEL_META_2, INFO_MODEL_META_3))
        whenever(turtleService.findInformationModel(INFO_MODEL_META_2.fdkId, true))
            .thenReturn(savedModel2)
        whenever(turtleService.findInformationModel(INFO_MODEL_META_3.fdkId, true))
            .thenReturn(savedModel3)

        whenever(valuesMock.catalogUri)
            .thenReturn("http://localhost:5000/catalogs")
        whenever(valuesMock.informationModelUri)
            .thenReturn("http://localhost:5000/informationmodels")

        harvester.harvestInformationModelCatalog(TEST_HARVEST_SOURCE_2, TEST_HARVEST_DATE)

        val harvestedModel = parseRDFResponse(harvested, JenaType.TURTLE, null)!!

        argumentCaptor<String, String>().apply {
            verify(turtleService, times(1)).saveOne(first.capture(), second.capture())
            assertEquals(TEST_HARVEST_SOURCE_2.url, first.firstValue)
            Assertions.assertTrue(checkIfIsomorphicAndPrintDiff(parseRDFResponse(second.firstValue, JenaType.TURTLE, null)!!, harvestedModel, "sourceWithCodeListIsParsedCorrectly-harvested"))
        }

        argumentCaptor<CatalogMeta>().apply {
            verify(catalogRepository, times(1)).save(capture())
            assertEquals(CATALOG_DBO_2, firstValue)
        }

        argumentCaptor<InformationModelMeta>().apply {
            verify(modelRepository, times(2)).save(capture())
            assertEquals(listOf(INFO_MODEL_META_2, INFO_MODEL_META_3), allValues.sortedBy { it.fdkId })
        }

        val catalog2 = responseReader.parseFile("catalog_2.ttl", "TURTLE")
        val model2 = parseRDFResponse(savedModel2, JenaType.TURTLE, null)!!
        val modelNoMeta2 = responseReader.parseFile("no_meta_model_2.ttl", "TURTLE")
        val model3 = parseRDFResponse(savedModel3, JenaType.TURTLE, null)!!
        val modelNoMeta3 = responseReader.parseFile("no_meta_model_3.ttl", "TURTLE")

        argumentCaptor<String, String, Boolean>().apply {
            verify(turtleService, times(2)).saveCatalog(first.capture(), second.capture(), third.capture())
            assertEquals(listOf(CATALOG_DBO_2.fdkId, CATALOG_DBO_2.fdkId), first.allValues)
            Assertions.assertTrue(checkIfIsomorphicAndPrintDiff(parseRDFResponse(second.firstValue, JenaType.TURTLE, null)!!, harvestedModel, "sourceWithCodeListIsParsedCorrectly-no-record-catalog"))
            Assertions.assertTrue(checkIfIsomorphicAndPrintDiff(parseRDFResponse(second.secondValue, JenaType.TURTLE, null)!!, catalog2, "sourceWithCodeListIsParsedCorrectly-record-catalog"))
            assertEquals(listOf(false, true), third.allValues)
        }

        argumentCaptor<String, String, Boolean>().apply {
            verify(turtleService, times(4)).saveInformationModel(first.capture(), second.capture(), third.capture())
            if (first.firstValue == INFO_MODEL_META_2.fdkId) {
                assertEquals(listOf(INFO_MODEL_META_2.fdkId, INFO_MODEL_META_2.fdkId, INFO_MODEL_META_3.fdkId, INFO_MODEL_META_3.fdkId), first.allValues)
                Assertions.assertTrue(checkIfIsomorphicAndPrintDiff(parseRDFResponse(second.allValues[0], JenaType.TURTLE, null)!!, modelNoMeta2, "sourceWithCodeListIsParsedCorrectly-model0"))
                Assertions.assertTrue(checkIfIsomorphicAndPrintDiff(parseRDFResponse(second.allValues[1], JenaType.TURTLE, null)!!, model2, "sourceWithCodeListIsParsedCorrectly-model1"))
                Assertions.assertTrue(checkIfIsomorphicAndPrintDiff(parseRDFResponse(second.allValues[2], JenaType.TURTLE, null)!!, modelNoMeta3, "sourceWithCodeListIsParsedCorrectly-model2"))
                Assertions.assertTrue(checkIfIsomorphicAndPrintDiff(parseRDFResponse(second.allValues[3], JenaType.TURTLE, null)!!, model3, "sourceWithCodeListIsParsedCorrectly-model3"))
            } else {
                assertEquals(listOf(INFO_MODEL_META_3.fdkId, INFO_MODEL_META_3.fdkId, INFO_MODEL_META_2.fdkId, INFO_MODEL_META_2.fdkId), first.allValues)
                Assertions.assertTrue(checkIfIsomorphicAndPrintDiff(parseRDFResponse(second.allValues[0], JenaType.TURTLE, null)!!, modelNoMeta3, "sourceWithCodeListIsParsedCorrectly-model4"))
                Assertions.assertTrue(checkIfIsomorphicAndPrintDiff(parseRDFResponse(second.allValues[1], JenaType.TURTLE, null)!!, model3, "sourceWithCodeListIsParsedCorrectly-model5"))
                Assertions.assertTrue(checkIfIsomorphicAndPrintDiff(parseRDFResponse(second.allValues[2], JenaType.TURTLE, null)!!, modelNoMeta2, "sourceWithCodeListIsParsedCorrectly-model6"))
                Assertions.assertTrue(checkIfIsomorphicAndPrintDiff(parseRDFResponse(second.allValues[3], JenaType.TURTLE, null)!!, model2, "sourceWithCodeListIsParsedCorrectly-model7"))

            }
            assertEquals(listOf(false, true, false, true), third.allValues)
        }
    }

    @Test
    fun harvestDataSourceNotPersistedWhenNoChangesFromDB() {
        val harvested = responseReader.readFile("harvest_response_0.ttl")
        whenever(adapter.getInformationModels(TEST_HARVEST_SOURCE))
            .thenReturn(harvested)

        whenever(turtleService.findOne(TEST_HARVEST_SOURCE.url!!))
            .thenReturn(harvested)

        whenever(valuesMock.catalogUri)
            .thenReturn("http://localhost:5000/catalogs")
        whenever(valuesMock.informationModelUri)
            .thenReturn("http://localhost:5000/informationmodels")

        harvester.harvestInformationModelCatalog(TEST_HARVEST_SOURCE, TEST_HARVEST_DATE)

        argumentCaptor<String, String>().apply {
            verify(turtleService, times(0)).saveOne(first.capture(), second.capture())
        }

        argumentCaptor<CatalogMeta>().apply {
            verify(catalogRepository, times(0)).save(capture())
        }

        argumentCaptor<InformationModelMeta>().apply {
            verify(modelRepository, times(0)).save(capture())
        }

        argumentCaptor<String, String, Boolean>().apply {
            verify(turtleService, times(0)).saveCatalog(first.capture(), second.capture(), third.capture())
        }

        argumentCaptor<String, String, Boolean>().apply {
            verify(turtleService, times(0)).saveInformationModel(first.capture(), second.capture(), third.capture())
        }

    }

    @Test
    fun onlyCatalogMetaUpdatedWhenOnlyCatalogDataChangedFromDB() {
        val harvested = responseReader.readFile("harvest_response_0.ttl")
        whenever(adapter.getInformationModels(TEST_HARVEST_SOURCE))
            .thenReturn(harvested)

        val catalogDiffTurtle = responseReader.readFile("harvest_response_0_catalog_diff.ttl")

        whenever(turtleService.findOne("http://localhost:5000/harvest"))
            .thenReturn(catalogDiffTurtle)
        whenever(catalogRepository.findById(CATALOG_DBO_0.uri))
            .thenReturn(Optional.of(CATALOG_DBO_0))
        whenever(modelRepository.findById(INFO_MODEL_DBO_0.uri))
            .thenReturn(Optional.of(INFO_MODEL_DBO_0))
        whenever(modelRepository.findAllByIsPartOf("http://localhost:5000/catalogs/$CATALOG_ID_0"))
            .thenReturn(listOf(INFO_MODEL_DBO_0))
        whenever(turtleService.findInformationModel(INFO_MODEL_ID_0, true))
            .thenReturn(responseReader.readFile("model_0.ttl"))
        whenever(turtleService.findInformationModel(INFO_MODEL_ID_0, false))
            .thenReturn(responseReader.readFile("no_meta_model_0.ttl"))

        whenever(valuesMock.catalogUri)
            .thenReturn("http://localhost:5000/catalogs")
        whenever(valuesMock.informationModelUri)
            .thenReturn("http://localhost:5000/informationmodels")

        val expectedCatalogDBO = CATALOG_DBO_0.copy(modified = NEW_TEST_HARVEST_DATE.timeInMillis)
        val expectedCatalogTurtle = responseReader.parseFile("catalog_0_catalog_diff.ttl", "TURTLE")
        val harvestedModel = parseRDFResponse(harvested, JenaType.TURTLE, null)!!

        harvester.harvestInformationModelCatalog(TEST_HARVEST_SOURCE, NEW_TEST_HARVEST_DATE)

        argumentCaptor<String, String>().apply {
            verify(turtleService, times(1)).saveOne(first.capture(), second.capture())
            assertEquals("http://localhost:5000/harvest", first.firstValue)
            Assertions.assertTrue(checkIfIsomorphicAndPrintDiff(parseRDFResponse(second.firstValue, JenaType.TURTLE, null)!!, harvestedModel, "onlyCatalogMetaUpdatedWhenOnlyCatalogDataChangedFromDB-harvested"))
        }

        argumentCaptor<CatalogMeta>().apply {
            verify(catalogRepository, times(1)).save(capture())
            assertEquals(expectedCatalogDBO, firstValue)
        }

        argumentCaptor<InformationModelMeta>().apply {
            verify(modelRepository, times(0)).save(capture())
        }

        argumentCaptor<String, String, Boolean>().apply {
            verify(turtleService, times(2)).saveCatalog(first.capture(), second.capture(), third.capture())
            assertEquals(listOf(CATALOG_DBO_0.fdkId, CATALOG_DBO_0.fdkId), first.allValues)
            Assertions.assertTrue(checkIfIsomorphicAndPrintDiff(parseRDFResponse(second.firstValue, JenaType.TURTLE, null)!!, harvestedModel, "onlyCatalogMetaUpdatedWhenOnlyCatalogDataChangedFromDB-no-record-catalog"))
            Assertions.assertTrue(checkIfIsomorphicAndPrintDiff(parseRDFResponse(second.secondValue, JenaType.TURTLE, null)!!, expectedCatalogTurtle, "onlyCatalogMetaUpdatedWhenOnlyCatalogDataChangedFromDB-record-catalog"))
            assertEquals(listOf(false, true), third.allValues)
        }

        argumentCaptor<String, String, Boolean>().apply {
            verify(turtleService, times(0)).saveInformationModel(first.capture(), second.capture(), third.capture())
        }
    }

    @Test
    fun harvestWithErrorsIsNotPersisted() {
        whenever(adapter.getInformationModels(TEST_HARVEST_SOURCE))
            .thenReturn(responseReader.readFile("harvest_error_response.ttl"))

        whenever(valuesMock.catalogUri)
            .thenReturn("http://localhost:5000/catalogs")
        whenever(valuesMock.informationModelUri)
            .thenReturn("http://localhost:5000/informationmodels")

        harvester.harvestInformationModelCatalog(TEST_HARVEST_SOURCE, TEST_HARVEST_DATE)

        argumentCaptor<String, String>().apply {
            verify(turtleService, times(0)).saveOne(first.capture(), second.capture())
        }

        argumentCaptor<CatalogMeta>().apply {
            verify(catalogRepository, times(0)).save(capture())
        }

        argumentCaptor<InformationModelMeta>().apply {
            verify(modelRepository, times(0)).save(capture())
        }

        argumentCaptor<String, String, Boolean>().apply {
            verify(turtleService, times(0)).saveCatalog(first.capture(), second.capture(), third.capture())
        }

        argumentCaptor<String, String, Boolean>().apply {
            verify(turtleService, times(0)).saveInformationModel(first.capture(), second.capture(), third.capture())
        }
    }

}