package no.fdk.fdk_informationmodel_harvester.harvester

import no.fdk.fdk_informationmodel_harvester.adapter.InformationModelAdapter
import no.fdk.fdk_informationmodel_harvester.configuration.ApplicationProperties
import no.fdk.fdk_informationmodel_harvester.model.CatalogMeta
import no.fdk.fdk_informationmodel_harvester.model.FdkIdAndUri
import no.fdk.fdk_informationmodel_harvester.model.HarvestReport
import no.fdk.fdk_informationmodel_harvester.model.InformationModelMeta
import no.fdk.fdk_informationmodel_harvester.rdf.parseRDFResponse
import no.fdk.fdk_informationmodel_harvester.repository.CatalogRepository
import no.fdk.fdk_informationmodel_harvester.repository.InformationModelRepository
import no.fdk.fdk_informationmodel_harvester.service.TurtleService
import no.fdk.fdk_informationmodel_harvester.utils.*
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
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
        whenever(modelRepository.findAllByIsPartOf("http://localhost:5050/catalogs/$CATALOG_ID_0"))
            .thenReturn(listOf(INFO_MODEL_DBO_0))
        whenever(turtleService.findInformationModel(INFO_MODEL_ID_0, true))
            .thenReturn(savedInfoModel)

        whenever(valuesMock.catalogUri)
            .thenReturn("http://localhost:5050/catalogs")
        whenever(valuesMock.informationModelUri)
            .thenReturn("http://localhost:5050/informationmodels")

        val report = harvester.harvestInformationModelCatalog(TEST_HARVEST_SOURCE, TEST_HARVEST_DATE, false)

        val harvestedModel = parseRDFResponse(harvested, Lang.TURTLE)

        argumentCaptor<String, String>().apply {
            verify(turtleService, times(1)).saveAsHarvestSource(first.capture(), second.capture())
            assertEquals(TEST_HARVEST_SOURCE.url, first.firstValue)
            Assertions.assertTrue(checkIfIsomorphicAndPrintDiff(parseRDFResponse(second.firstValue, Lang.TURTLE), harvestedModel, "harvestDataSourceSavedWhenDBIsEmpty-harvested"))
        }

        argumentCaptor<CatalogMeta>().apply {
            verify(catalogRepository, times(1)).save(capture())
            assertEquals(CATALOG_DBO_0, firstValue)
        }

        argumentCaptor<InformationModelMeta>().apply {
            verify(modelRepository, times(1)).save(capture())
            assertEquals(INFO_MODEL_DBO_0, firstValue)
        }

        val catalog0NoMeta = responseReader.parseFile("no_meta_catalog_0.ttl", "TURTLE")
        val model0NoMeta = responseReader.parseFile("no_meta_model_0.ttl", "TURTLE")

        argumentCaptor<String, String, Boolean>().apply {
            verify(turtleService, times(1)).saveCatalog(first.capture(), second.capture(), third.capture())
            assertEquals(listOf(CATALOG_ID_0), first.allValues)
            Assertions.assertTrue(checkIfIsomorphicAndPrintDiff(parseRDFResponse(second.firstValue, Lang.TURTLE), catalog0NoMeta, "harvestDataSourceSavedWhenDBIsEmpty-no-record-catalog"))
            assertEquals(listOf(false), third.allValues)
        }

        argumentCaptor<String, String, Boolean>().apply {
            verify(turtleService, times(1)).saveInformationModel(first.capture(), second.capture(), third.capture())
            assertEquals(listOf(INFO_MODEL_ID_0), first.allValues)
            Assertions.assertTrue(checkIfIsomorphicAndPrintDiff(parseRDFResponse(second.firstValue, Lang.TURTLE), model0NoMeta, "harvestDataSourceSavedWhenDBIsEmpty-record-model"))
            assertEquals(listOf(false), third.allValues)
        }

        val expectedReport = HarvestReport(
            id="harvest",
            url="http://localhost:5050/harvest",
            dataType="informationmodel",
            harvestError=false,
            startTime = "2020-10-05 15:15:39 +0200",
            endTime = report!!.endTime,
            changedCatalogs = listOf(FdkIdAndUri(
                fdkId="e5b2ad5e-078b-3aea-af04-6051c2b0244b",
                uri="https://raw.githubusercontent.com/Informasjonsforvaltning/model-publisher/master/src/model/model-catalog.ttl#Katalog0")),
            changedResources = listOf(FdkIdAndUri(
                fdkId="409c97dd-57e0-3a29-b5a3-023733cf5064",
                uri="https://raw.githubusercontent.com/Informasjonsforvaltning/model-publisher/master/src/model/model-catalog.ttl#PersonOgEnhet"))
        )

        kotlin.test.assertEquals(expectedReport, report)
    }

    @Test
    fun sourceWithCodeListIsParsedCorrectly() {
        val harvested = responseReader.readFile("harvest_response_2.ttl")
        val savedModel2 = responseReader.readFile("model_2.ttl")
        val savedModel3 = responseReader.readFile("model_3.ttl")

        whenever(adapter.getInformationModels(TEST_HARVEST_SOURCE_2))
            .thenReturn(harvested)
        whenever(modelRepository.findAllByIsPartOf("http://localhost:5050/catalogs/$CATALOG_ID_2"))
            .thenReturn(listOf(INFO_MODEL_META_2, INFO_MODEL_META_3))
        whenever(turtleService.findInformationModel(INFO_MODEL_META_2.fdkId, true))
            .thenReturn(savedModel2)
        whenever(turtleService.findInformationModel(INFO_MODEL_META_3.fdkId, true))
            .thenReturn(savedModel3)

        whenever(valuesMock.catalogUri)
            .thenReturn("http://localhost:5050/catalogs")
        whenever(valuesMock.informationModelUri)
            .thenReturn("http://localhost:5050/informationmodels")

        val report = harvester.harvestInformationModelCatalog(TEST_HARVEST_SOURCE_2, TEST_HARVEST_DATE, false)

        val harvestedModel = parseRDFResponse(harvested, Lang.TURTLE)

        argumentCaptor<String, String>().apply {
            verify(turtleService, times(1)).saveAsHarvestSource(first.capture(), second.capture())
            assertEquals(TEST_HARVEST_SOURCE_2.url, first.firstValue)
            Assertions.assertTrue(checkIfIsomorphicAndPrintDiff(parseRDFResponse(second.firstValue, Lang.TURTLE), harvestedModel, "sourceWithCodeListIsParsedCorrectly-harvested"))
        }

        argumentCaptor<CatalogMeta>().apply {
            verify(catalogRepository, times(1)).save(capture())
            assertEquals(CATALOG_DBO_2, firstValue)
        }

        argumentCaptor<InformationModelMeta>().apply {
            verify(modelRepository, times(2)).save(capture())
            assertEquals(listOf(INFO_MODEL_META_2, INFO_MODEL_META_3), allValues.sortedBy { it.fdkId })
        }

        val catalogNoMeta2 = responseReader.parseFile("no_meta_catalog_2.ttl", "TURTLE")
        val modelNoMeta2 = responseReader.parseFile("no_meta_model_2.ttl", "TURTLE")
        val modelNoMeta3 = responseReader.parseFile("no_meta_model_3.ttl", "TURTLE")

        argumentCaptor<String, String, Boolean>().apply {
            verify(turtleService, times(1)).saveCatalog(first.capture(), second.capture(), third.capture())
            assertEquals(listOf(CATALOG_DBO_2.fdkId), first.allValues)
            Assertions.assertTrue(checkIfIsomorphicAndPrintDiff(parseRDFResponse(second.firstValue, Lang.TURTLE), catalogNoMeta2, "sourceWithCodeListIsParsedCorrectly-no-record-catalog"))
            assertEquals(listOf(false), third.allValues)
        }

        argumentCaptor<String, String, Boolean>().apply {
            verify(turtleService, times(2)).saveInformationModel(first.capture(), second.capture(), third.capture())
            if (first.firstValue == INFO_MODEL_META_2.fdkId) {
                assertEquals(listOf(INFO_MODEL_META_2.fdkId, INFO_MODEL_META_3.fdkId), first.allValues)
                Assertions.assertTrue(checkIfIsomorphicAndPrintDiff(parseRDFResponse(second.allValues[0], Lang.TURTLE), modelNoMeta2, "sourceWithCodeListIsParsedCorrectly-model0"))
                Assertions.assertTrue(checkIfIsomorphicAndPrintDiff(parseRDFResponse(second.allValues[1], Lang.TURTLE), modelNoMeta3, "sourceWithCodeListIsParsedCorrectly-model2"))
            } else {
                assertEquals(listOf(INFO_MODEL_META_3.fdkId, INFO_MODEL_META_2.fdkId), first.allValues)
                Assertions.assertTrue(checkIfIsomorphicAndPrintDiff(parseRDFResponse(second.allValues[0], Lang.TURTLE), modelNoMeta3, "sourceWithCodeListIsParsedCorrectly-model4"))
                Assertions.assertTrue(checkIfIsomorphicAndPrintDiff(parseRDFResponse(second.allValues[1], Lang.TURTLE), modelNoMeta2, "sourceWithCodeListIsParsedCorrectly-model6"))
            }
            assertEquals(listOf(false, false), third.allValues)
        }

        val expectedReport = HarvestReport(
            id="harvest2",
            url="http://localhost:5050/harvest2",
            dataType="informationmodel",
            harvestError=false,
            startTime = "2020-10-05 15:15:39 +0200",
            endTime = report!!.endTime,
            changedCatalogs = listOf(FdkIdAndUri(
                fdkId="f25c939d-0722-3aa3-82b1-eaa457086444",
                uri="https://raw.github.com/Informasjonsforvaltning/model-publisher/master/src/model/model-catalog.ttl#Katalog2")),
            changedResources = listOf(
                FdkIdAndUri(fdkId="bcbe6738-85f6-388c-afcc-ef1fafd7cc45", uri="https://raw.github.com/Informasjonsforvaltning/model-publisher/master/src/model/model-catalog.ttl#Diversemodell"),
                FdkIdAndUri(fdkId="0bf6b09f-e1c0-3415-bba0-7ff2edada89d", uri="https://raw.github.com/Informasjonsforvaltning/model-publisher/master/src/model/model-catalog.ttl#AltMuligModell"))
        )

        kotlin.test.assertEquals(expectedReport, report)
    }

    @Test
    fun harvestDataSourceNotPersistedWhenNoChangesFromDB() {
        val harvested = responseReader.readFile("harvest_response_0.ttl")
        whenever(adapter.getInformationModels(TEST_HARVEST_SOURCE))
            .thenReturn(harvested)

        whenever(turtleService.findHarvestSource(TEST_HARVEST_SOURCE.url!!))
            .thenReturn(harvested)

        whenever(valuesMock.catalogUri)
            .thenReturn("http://localhost:5050/catalogs")
        whenever(valuesMock.informationModelUri)
            .thenReturn("http://localhost:5050/informationmodels")

        val report = harvester.harvestInformationModelCatalog(TEST_HARVEST_SOURCE, TEST_HARVEST_DATE, false)

        verify(turtleService, times(0)).saveAsHarvestSource(any(), any())
        verify(catalogRepository, times(0)).save(any())
        verify(modelRepository, times(0)).save(any())
        verify(turtleService, times(0)).saveCatalog(any(), any(), any())
        verify(turtleService, times(0)).saveInformationModel(any(), any(), any())

        val expectedReport = HarvestReport(
            id="harvest",
            url="http://localhost:5050/harvest",
            dataType="informationmodel",
            harvestError=false,
            startTime = "2020-10-05 15:15:39 +0200",
            endTime = report!!.endTime
        )

        kotlin.test.assertEquals(expectedReport, report)
    }

    @Test
    fun noChangesIgnoredWhenForceUpdateIsTrue() {
        val harvested = responseReader.readFile("harvest_response_0.ttl")
        whenever(adapter.getInformationModels(TEST_HARVEST_SOURCE))
            .thenReturn(harvested)
        whenever(turtleService.findHarvestSource(TEST_HARVEST_SOURCE.url!!))
            .thenReturn(harvested)
        whenever(modelRepository.findById(INFO_MODEL_DBO_0.uri))
            .thenReturn(Optional.of(INFO_MODEL_DBO_0))
        whenever(modelRepository.findAllByIsPartOf("http://localhost:5050/catalogs/$CATALOG_ID_0"))
            .thenReturn(listOf(INFO_MODEL_DBO_0))
        whenever(turtleService.findInformationModel(INFO_MODEL_ID_0, withRecords = false))
            .thenReturn(responseReader.readFile("no_meta_model_0.ttl"))

        whenever(valuesMock.catalogUri)
            .thenReturn("http://localhost:5050/catalogs")
        whenever(valuesMock.informationModelUri)
            .thenReturn("http://localhost:5050/informationmodels")

        val report = harvester.harvestInformationModelCatalog(TEST_HARVEST_SOURCE, TEST_HARVEST_DATE, true)

        verify(turtleService, times(1)).saveAsHarvestSource(any(), any())
        verify(modelRepository, times(0)).save(any())
        verify(turtleService, times(1)).saveCatalog(any(), any(), any())
        verify(turtleService, times(1)).saveInformationModel(any(), any(), any())

        val expectedReport = HarvestReport(
            id="harvest",
            url="http://localhost:5050/harvest",
            dataType="informationmodel",
            harvestError=false,
            startTime = "2020-10-05 15:15:39 +0200",
            endTime = report!!.endTime,
            changedCatalogs = listOf(FdkIdAndUri(fdkId="e5b2ad5e-078b-3aea-af04-6051c2b0244b", uri="https://raw.githubusercontent.com/Informasjonsforvaltning/model-publisher/master/src/model/model-catalog.ttl#Katalog0")),
            changedResources = listOf(FdkIdAndUri(fdkId="409c97dd-57e0-3a29-b5a3-023733cf5064", uri="https://raw.githubusercontent.com/Informasjonsforvaltning/model-publisher/master/src/model/model-catalog.ttl#PersonOgEnhet"))
        )

        kotlin.test.assertEquals(expectedReport, report)
    }

    @Test
    fun onlyCatalogMetaUpdatedWhenOnlyCatalogDataChangedFromDB() {
        val harvested = responseReader.readFile("harvest_response_0.ttl")
        whenever(adapter.getInformationModels(TEST_HARVEST_SOURCE))
            .thenReturn(harvested)

        val catalogDiffTurtle = responseReader.readFile("harvest_response_0_catalog_diff.ttl")

        whenever(turtleService.findHarvestSource("http://localhost:5050/harvest"))
            .thenReturn(catalogDiffTurtle)
        whenever(catalogRepository.findById(CATALOG_DBO_0.uri))
            .thenReturn(Optional.of(CATALOG_DBO_0))
        whenever(modelRepository.findById(INFO_MODEL_DBO_0.uri))
            .thenReturn(Optional.of(INFO_MODEL_DBO_0))
        whenever(modelRepository.findAllByIsPartOf("http://localhost:5050/catalogs/$CATALOG_ID_0"))
            .thenReturn(listOf(INFO_MODEL_DBO_0))
        whenever(turtleService.findInformationModel(INFO_MODEL_ID_0, true))
            .thenReturn(responseReader.readFile("model_0.ttl"))
        whenever(turtleService.findInformationModel(INFO_MODEL_ID_0, false))
            .thenReturn(responseReader.readFile("no_meta_model_0.ttl"))

        whenever(valuesMock.catalogUri)
            .thenReturn("http://localhost:5050/catalogs")
        whenever(valuesMock.informationModelUri)
            .thenReturn("http://localhost:5050/informationmodels")

        val expectedCatalogDBO = CATALOG_DBO_0.copy(modified = NEW_TEST_HARVEST_DATE.timeInMillis)
        val expectedNoMetaCatalog = responseReader.parseFile("no_meta_catalog_0.ttl", "TURTLE")
        val harvestedModel = parseRDFResponse(harvested, Lang.TURTLE)

        val report = harvester.harvestInformationModelCatalog(TEST_HARVEST_SOURCE, NEW_TEST_HARVEST_DATE, false)

        argumentCaptor<String, String>().apply {
            verify(turtleService, times(1)).saveAsHarvestSource(first.capture(), second.capture())
            assertEquals("http://localhost:5050/harvest", first.firstValue)
            Assertions.assertTrue(checkIfIsomorphicAndPrintDiff(parseRDFResponse(second.firstValue, Lang.TURTLE), harvestedModel, "onlyCatalogMetaUpdatedWhenOnlyCatalogDataChangedFromDB-harvested"))
        }

        argumentCaptor<CatalogMeta>().apply {
            verify(catalogRepository, times(1)).save(capture())
            assertEquals(expectedCatalogDBO, firstValue)
        }

        argumentCaptor<InformationModelMeta>().apply {
            verify(modelRepository, times(0)).save(capture())
        }

        argumentCaptor<String, String, Boolean>().apply {
            verify(turtleService, times(1)).saveCatalog(first.capture(), second.capture(), third.capture())
            assertEquals(listOf(CATALOG_DBO_0.fdkId), first.allValues)
            Assertions.assertTrue(checkIfIsomorphicAndPrintDiff(parseRDFResponse(second.firstValue, Lang.TURTLE), expectedNoMetaCatalog, "onlyCatalogMetaUpdatedWhenOnlyCatalogDataChangedFromDB-no-record-catalog"))
            assertEquals(listOf(false), third.allValues)
        }

        argumentCaptor<String, String, Boolean>().apply {
            verify(turtleService, times(0)).saveInformationModel(first.capture(), second.capture(), third.capture())
        }

        val expectedReport = HarvestReport(
            id="harvest",
            url="http://localhost:5050/harvest",
            dataType="informationmodel",
            harvestError=false,
            startTime = "2020-10-15 13:52:16 +0200",
            endTime = report!!.endTime,
            changedCatalogs = listOf(FdkIdAndUri(
                fdkId="e5b2ad5e-078b-3aea-af04-6051c2b0244b",
                uri="https://raw.githubusercontent.com/Informasjonsforvaltning/model-publisher/master/src/model/model-catalog.ttl#Katalog0"))
        )

        kotlin.test.assertEquals(expectedReport, report)
    }

    @Test
    fun harvestWithErrorsIsNotPersisted() {
        whenever(adapter.getInformationModels(TEST_HARVEST_SOURCE))
            .thenReturn(responseReader.readFile("harvest_error_response.ttl"))

        whenever(valuesMock.catalogUri)
            .thenReturn("http://localhost:5050/catalogs")
        whenever(valuesMock.informationModelUri)
            .thenReturn("http://localhost:5050/informationmodels")

        val report = harvester.harvestInformationModelCatalog(TEST_HARVEST_SOURCE, TEST_HARVEST_DATE, false)

        verify(turtleService, times(0)).saveAsHarvestSource(any(), any())
        verify(catalogRepository, times(0)).save(any())
        verify(modelRepository, times(0)).save(any())
        verify(turtleService, times(0)).saveCatalog(any(), any(), any())
        verify(turtleService, times(0)).saveInformationModel(any(), any(), any())

        val expectedReport = HarvestReport(
            id="harvest",
            url="http://localhost:5050/harvest",
            dataType="informationmodel",
            harvestError=true,
            errorMessage = "[line: 1, col: 1 ] Undefined prefix: digdir",
            startTime = "2020-10-05 15:15:39 +0200",
            endTime = report!!.endTime
        )

        kotlin.test.assertEquals(expectedReport, report)
    }

    @Test
    fun removedModelsUpdatedAndAddedToReport() {
        val harvested = responseReader.readFile("harvest_response_0_old_model_removed.ttl")
        val old = responseReader.readFile("harvest_response_0.ttl")
        whenever(adapter.getInformationModels(TEST_HARVEST_SOURCE))
            .thenReturn(harvested)
        whenever(turtleService.findHarvestSource(TEST_HARVEST_SOURCE.url!!))
            .thenReturn(old)
        whenever(modelRepository.findAllByIsPartOf("http://localhost:5050/catalogs/$CATALOG_ID_0"))
            .thenReturn(listOf(INFO_MODEL_DBO_0))

        whenever(valuesMock.catalogUri)
            .thenReturn("http://localhost:5050/catalogs")
        whenever(valuesMock.informationModelUri)
            .thenReturn("http://localhost:5050/informationmodels")

        val report = harvester.harvestInformationModelCatalog(TEST_HARVEST_SOURCE, TEST_HARVEST_DATE, false)

        argumentCaptor<List<InformationModelMeta>>().apply {
            verify(modelRepository, times(1)).saveAll(capture())
            assertEquals(listOf(INFO_MODEL_DBO_0.copy(removed = true)), firstValue)
        }

        val expectedReport = HarvestReport(
            id="harvest",
            url="http://localhost:5050/harvest",
            dataType="informationmodel",
            harvestError=false,
            startTime = "2020-10-05 15:15:39 +0200",
            endTime = report!!.endTime,
            errorMessage=null,
            changedCatalogs=listOf(FdkIdAndUri(fdkId="e5b2ad5e-078b-3aea-af04-6051c2b0244b", uri="https://raw.githubusercontent.com/Informasjonsforvaltning/model-publisher/master/src/model/model-catalog.ttl#Katalog0")),
            changedResources=listOf(FdkIdAndUri(fdkId="0ed14280-a5a8-3886-a1b5-37e9fe5df314", uri="https://raw.githubusercontent.com/Informasjonsforvaltning/model-publisher/master/src/model/model-catalog.ttl#Ny")),
            removedResources = listOf(FdkIdAndUri(fdkId="409c97dd-57e0-3a29-b5a3-023733cf5064", uri="https://raw.githubusercontent.com/Informasjonsforvaltning/model-publisher/master/src/model/model-catalog.ttl#PersonOgEnhet"))
        )

        assertEquals(expectedReport, report)
    }

    @Test
    fun allowEmptyCatalog() {
        val harvested = responseReader.readFile("harvest_response_0_empty.ttl")
        val old = responseReader.readFile("harvest_response_0.ttl")
        whenever(adapter.getInformationModels(TEST_HARVEST_SOURCE))
            .thenReturn(harvested)
        whenever(turtleService.findHarvestSource(TEST_HARVEST_SOURCE.url!!))
            .thenReturn(old)
        whenever(modelRepository.findAllByIsPartOf("http://localhost:5050/catalogs/$CATALOG_ID_0"))
            .thenReturn(listOf(INFO_MODEL_DBO_0))

        whenever(valuesMock.catalogUri)
            .thenReturn("http://localhost:5050/catalogs")
        whenever(valuesMock.informationModelUri)
            .thenReturn("http://localhost:5050/informationmodels")

        val report = harvester.harvestInformationModelCatalog(TEST_HARVEST_SOURCE, TEST_HARVEST_DATE, false)

        val expectedReport = HarvestReport(
            id="harvest",
            url="http://localhost:5050/harvest",
            dataType="informationmodel",
            harvestError=false,
            startTime = "2020-10-05 15:15:39 +0200",
            endTime = report!!.endTime,
            errorMessage=null,
            changedCatalogs=listOf(FdkIdAndUri(fdkId="e5b2ad5e-078b-3aea-af04-6051c2b0244b", uri="https://raw.githubusercontent.com/Informasjonsforvaltning/model-publisher/master/src/model/model-catalog.ttl#Katalog0")),
            changedResources= emptyList(),
            removedResources = listOf(FdkIdAndUri(fdkId="409c97dd-57e0-3a29-b5a3-023733cf5064", uri="https://raw.githubusercontent.com/Informasjonsforvaltning/model-publisher/master/src/model/model-catalog.ttl#PersonOgEnhet"))
        )

        assertEquals(expectedReport, report)
    }

    @Test
    fun earlierRemovedInfoModelWithNoChangesAddedToReport() {
        val harvested = responseReader.readFile("harvest_response_0.ttl")
        whenever(adapter.getInformationModels(TEST_HARVEST_SOURCE))
            .thenReturn(harvested)
        whenever(turtleService.findHarvestSource(TEST_HARVEST_SOURCE.url!!))
            .thenReturn(responseReader.readFile("harvest_response_0_old_model_removed.ttl"))
        whenever(modelRepository.findById(INFO_MODEL_DBO_0.uri))
            .thenReturn(Optional.of(INFO_MODEL_DBO_0.copy(removed = true)))
        whenever(modelRepository.findAllByIsPartOf("http://localhost:5050/catalogs/$CATALOG_ID_0"))
            .thenReturn(listOf(INFO_MODEL_DBO_0.copy(removed = true)))
        whenever(turtleService.findInformationModel(INFO_MODEL_ID_0, withRecords = false))
            .thenReturn(responseReader.readFile("no_meta_model_0.ttl"))

        whenever(valuesMock.catalogUri)
            .thenReturn("http://localhost:5050/catalogs")
        whenever(valuesMock.informationModelUri)
            .thenReturn("http://localhost:5050/informationmodels")

        val report = harvester.harvestInformationModelCatalog(TEST_HARVEST_SOURCE, TEST_HARVEST_DATE, false)

        argumentCaptor<InformationModelMeta>().apply {
            verify(modelRepository, times(1)).save(capture())
            assertEquals(INFO_MODEL_DBO_0, firstValue)
        }

        val expectedReport = HarvestReport(
            id="harvest",
            url="http://localhost:5050/harvest",
            dataType="informationmodel",
            harvestError=false,
            startTime = "2020-10-05 15:15:39 +0200",
            endTime = report!!.endTime,
            errorMessage=null,
            changedCatalogs=listOf(FdkIdAndUri(fdkId="e5b2ad5e-078b-3aea-af04-6051c2b0244b", uri="https://raw.githubusercontent.com/Informasjonsforvaltning/model-publisher/master/src/model/model-catalog.ttl#Katalog0")),
            changedResources=listOf(FdkIdAndUri(fdkId="409c97dd-57e0-3a29-b5a3-023733cf5064", uri="https://raw.githubusercontent.com/Informasjonsforvaltning/model-publisher/master/src/model/model-catalog.ttl#PersonOgEnhet"))
        )

        assertEquals(expectedReport, report)
    }

}
