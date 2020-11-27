package no.fdk.fdk_informationmodel_harvester.harvester

import com.nhaarman.mockitokotlin2.*
import no.fdk.fdk_informationmodel_harvester.adapter.InformationModelAdapter
import no.fdk.fdk_informationmodel_harvester.configuration.ApplicationProperties
import no.fdk.fdk_informationmodel_harvester.model.CatalogDBO
import no.fdk.fdk_informationmodel_harvester.model.InformationModelDBO
import no.fdk.fdk_informationmodel_harvester.model.MiscellaneousTurtle
import no.fdk.fdk_informationmodel_harvester.repository.CatalogRepository
import no.fdk.fdk_informationmodel_harvester.repository.InformationModelRepository
import no.fdk.fdk_informationmodel_harvester.repository.MiscellaneousRepository
import no.fdk.fdk_informationmodel_harvester.service.gzip
import no.fdk.fdk_informationmodel_harvester.utils.*
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

@Tag("unit")
class HarvesterTest {
    private val catalogRepository: CatalogRepository = mock()
    private val modelRepository: InformationModelRepository = mock()
    private val miscRepository: MiscellaneousRepository = mock()
    private val valuesMock: ApplicationProperties = mock()
    private val adapter: InformationModelAdapter = mock()

    private val harvester = InformationModelHarvester(
        adapter, catalogRepository,
        modelRepository, miscRepository, valuesMock
    )

    private val responseReader = TestResponseReader()

    @Test
    fun harvestDataSourceSavedWhenDBIsEmpty() {
        whenever(adapter.getInformationModels(TEST_HARVEST_SOURCE))
            .thenReturn(responseReader.readFile("harvest_response_0.ttl"))

        whenever(valuesMock.catalogUri)
            .thenReturn("http://localhost:5000/catalogs")
        whenever(valuesMock.informationModelUri)
            .thenReturn("http://localhost:5000/informationmodels")

        harvester.harvestInformationModelCatalog(TEST_HARVEST_SOURCE, TEST_HARVEST_DATE)

        argumentCaptor<MiscellaneousTurtle>().apply {
            verify(miscRepository, times(1)).save(capture())
            if (HARVEST_DBO_0 != firstValue) firstValue.printTurtleDiff(HARVEST_DBO_0)
            assertEquals(HARVEST_DBO_0, firstValue)
        }

        argumentCaptor<List<CatalogDBO>>().apply {
            verify(catalogRepository, times(1)).saveAll(capture())
            assertEquals(1, firstValue.size)
            if (CATALOG_DBO_0 != firstValue.first()) firstValue.first().printTurtleDiff(CATALOG_DBO_0)
            assertEquals(CATALOG_DBO_0, firstValue.first())
        }

        argumentCaptor<List<InformationModelDBO>>().apply {
            verify(modelRepository, times(1)).saveAll(capture())
            assertEquals(1, firstValue.size)
            if (INFO_MODEL_DBO_0 != firstValue.first()) firstValue.first().printTurtleDiff(INFO_MODEL_DBO_0)
            assertEquals(INFO_MODEL_DBO_0, firstValue.first())
        }
    }

    @Test
    fun sourceWithCodeListIsParsedCorrectly() {
        whenever(adapter.getInformationModels(TEST_HARVEST_SOURCE_2))
            .thenReturn(responseReader.readFile("harvest_response_2.ttl"))

        whenever(valuesMock.catalogUri)
            .thenReturn("http://localhost:5000/catalogs")
        whenever(valuesMock.informationModelUri)
            .thenReturn("http://localhost:5000/informationmodels")

        harvester.harvestInformationModelCatalog(TEST_HARVEST_SOURCE_2, TEST_HARVEST_DATE)

        argumentCaptor<MiscellaneousTurtle>().apply {
            verify(miscRepository, times(1)).save(capture())
            if (HARVEST_DBO_2 != firstValue) firstValue.printTurtleDiff(HARVEST_DBO_2)
            assertEquals(HARVEST_DBO_2, firstValue)
        }

        argumentCaptor<List<CatalogDBO>>().apply {
            verify(catalogRepository, times(1)).saveAll(capture())
            assertEquals(1, firstValue.size)
            if (CATALOG_DBO_2 != firstValue.first()) firstValue.first().printTurtleDiff(CATALOG_DBO_2)
            assertEquals(CATALOG_DBO_2, firstValue.first())
        }

        argumentCaptor<List<InformationModelDBO>>().apply {
            verify(modelRepository, times(1)).saveAll(capture())
            assertEquals(2, firstValue.size)
        }
    }

    @Test
    fun harvestDataSourceNotPersistedWhenNoChangesFromDB() {
        whenever(adapter.getInformationModels(TEST_HARVEST_SOURCE))
            .thenReturn(responseReader.readFile("harvest_response_0.ttl"))

        whenever(miscRepository.findById(TEST_HARVEST_SOURCE.url!!))
            .thenReturn(Optional.of(HARVEST_DBO_0))

        whenever(valuesMock.catalogUri)
            .thenReturn("http://localhost:5000/catalogs")
        whenever(valuesMock.informationModelUri)
            .thenReturn("http://localhost:5000/informationmodels")

        harvester.harvestInformationModelCatalog(TEST_HARVEST_SOURCE, TEST_HARVEST_DATE)

        argumentCaptor<MiscellaneousTurtle>().apply {
            verify(miscRepository, times(0)).save(capture())
        }

        argumentCaptor<List<CatalogDBO>>().apply {
            verify(catalogRepository, times(0)).saveAll(capture())
        }

        argumentCaptor<List<InformationModelDBO>>().apply {
            verify(modelRepository, times(0)).saveAll(capture())
        }

    }

    @Test
    fun onlyCatalogMetaUpdatedWhenOnlyCatalogDataChangedFromDB() {
        whenever(adapter.getInformationModels(TEST_HARVEST_SOURCE))
            .thenReturn(responseReader.readFile("harvest_response_0.ttl"))

        val catalogDiffTurtle = gzip(responseReader.readFile("harvest_response_0_catalog_diff.ttl"))

        whenever(miscRepository.findById("http://localhost:5000/harvest0"))
            .thenReturn(Optional.of(HARVEST_DBO_0.copy(turtle = catalogDiffTurtle)))

        whenever(catalogRepository.findById(CATALOG_DBO_0.uri))
            .thenReturn(Optional.of(CATALOG_DBO_0.copy(turtleHarvested = catalogDiffTurtle)))

        whenever(modelRepository.findById(INFO_MODEL_DBO_0.uri))
            .thenReturn(Optional.of(INFO_MODEL_DBO_0))

        whenever(valuesMock.catalogUri)
            .thenReturn("http://localhost:5000/catalogs")
        whenever(valuesMock.informationModelUri)
            .thenReturn("http://localhost:5000/informationmodels")

        val expectedCatalogDBO = CATALOG_DBO_0.copy(
            modified = NEW_TEST_HARVEST_DATE.timeInMillis,
            turtleCatalog = gzip(responseReader.readFile("catalog_0_catalog_diff.ttl"))
        )

        harvester.harvestInformationModelCatalog(TEST_HARVEST_SOURCE, NEW_TEST_HARVEST_DATE)

        argumentCaptor<MiscellaneousTurtle>().apply {
            verify(miscRepository, times(1)).save(capture())
            if (HARVEST_DBO_0 != firstValue) firstValue.printTurtleDiff(HARVEST_DBO_0)
            assertEquals(HARVEST_DBO_0, firstValue)
        }

        argumentCaptor<List<CatalogDBO>>().apply {
            verify(catalogRepository, times(1)).saveAll(capture())
            assertEquals(1, firstValue.size)
            if (expectedCatalogDBO != firstValue.first()) firstValue.first().printTurtleDiff(expectedCatalogDBO)
            assertEquals(expectedCatalogDBO, firstValue.first())
        }

        argumentCaptor<List<InformationModelDBO>>().apply {
            verify(modelRepository, times(1)).saveAll(capture())
            assertEquals(0, firstValue.size)
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

        argumentCaptor<MiscellaneousTurtle>().apply {
            verify(miscRepository, times(0)).save(capture())
        }

        argumentCaptor<List<CatalogDBO>>().apply {
            verify(catalogRepository, times(0)).saveAll(capture())
        }

        argumentCaptor<List<InformationModelDBO>>().apply {
            verify(modelRepository, times(0)).saveAll(capture())
        }
    }

}