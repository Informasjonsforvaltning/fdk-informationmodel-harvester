package no.fdk.fdk_informationmodel_harvester.service

import com.nhaarman.mockitokotlin2.*
import no.fdk.fdk_informationmodel_harvester.adapter.FusekiAdapter
import no.fdk.fdk_informationmodel_harvester.configuration.ApplicationProperties
import no.fdk.fdk_informationmodel_harvester.configuration.FusekiProperties
import no.fdk.fdk_informationmodel_harvester.model.*
import no.fdk.fdk_informationmodel_harvester.rdf.JenaType
import no.fdk.fdk_informationmodel_harvester.rdf.createRDFResponse
import no.fdk.fdk_informationmodel_harvester.repository.CatalogRepository
import no.fdk.fdk_informationmodel_harvester.repository.InformationModelRepository
import no.fdk.fdk_informationmodel_harvester.repository.MiscellaneousRepository
import no.fdk.fdk_informationmodel_harvester.utils.*
import org.apache.jena.rdf.model.Model
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Tag("unit")
class UpdateServiceTest {
    private val catalogRepository: CatalogRepository = mock()
    private val modelRepository: InformationModelRepository = mock()
    private val miscRepository: MiscellaneousRepository = mock()
    private val valuesMock: ApplicationProperties = mock()
    private val fusekiAdapter: FusekiAdapter = mock()
    private val updateService = UpdateService(
        valuesMock, fusekiAdapter, catalogRepository, modelRepository, miscRepository)

    private val responseReader = TestResponseReader()

    @Nested
    internal inner class UpdateMetaData {

        @Test
        fun noDiffInMetaDataLeavesTurtleUnchanged() {
            whenever(catalogRepository.findAll())
                .thenReturn(listOf(CATALOG_DBO_0, CATALOG_DBO_1))
            whenever(modelRepository.findAllByIsPartOf("http://localhost:5000/catalogs/${CATALOG_DBO_0.fdkId}"))
                .thenReturn(listOf(INFO_MODEL_DBO_0))
            whenever(modelRepository.findAllByIsPartOf("http://localhost:5000/catalogs/${CATALOG_DBO_1.fdkId}"))
                .thenReturn(listOf(INFO_MODEL_DBO_1))

            whenever(valuesMock.catalogUri)
                .thenReturn("http://localhost:5000/catalogs")
            whenever(valuesMock.informationModelUri)
                .thenReturn("http://localhost:5000/informationmodels")

            updateService.updateMetaData()

            argumentCaptor<List<CatalogDBO>>().apply {
                verify(catalogRepository, times(1)).saveAll(capture())
                if (CATALOG_DBO_0 != firstValue.first()) firstValue.first().printTurtleDiff(CATALOG_DBO_0)
                if (CATALOG_DBO_1 != firstValue[1]) firstValue[1].printTurtleDiff(CATALOG_DBO_0)
                assertEquals(listOf(CATALOG_DBO_0, CATALOG_DBO_1), firstValue)
            }

            argumentCaptor<List<InformationModelDBO>>().apply {
                verify(modelRepository, times(1)).saveAll(capture())
                if (INFO_MODEL_DBO_0 != firstValue.first()) firstValue.first().printTurtleDiff(INFO_MODEL_DBO_0)
                if (INFO_MODEL_DBO_1 != firstValue[1]) firstValue[1].printTurtleDiff(INFO_MODEL_DBO_1)
                assertEquals(listOf(INFO_MODEL_DBO_0, INFO_MODEL_DBO_1), firstValue)
            }
        }

        @Test
        fun diffInMetaDataUpdatesTurtle() {
            val catalogWrongMeta = gzip(responseReader.readFile("catalog_1_wrong_meta.ttl"))
            val modelWrongMeta = gzip(responseReader.readFile("model_1_wrong_meta.ttl"))

            whenever(catalogRepository.findAll())
                .thenReturn(listOf(CATALOG_DBO_0, CATALOG_DBO_1.copy(turtleCatalog = catalogWrongMeta)))
            whenever(modelRepository.findAllByIsPartOf("http://localhost:5000/catalogs/${CATALOG_DBO_0.fdkId}"))
                .thenReturn(listOf(INFO_MODEL_DBO_0))
            whenever(modelRepository.findAllByIsPartOf("http://localhost:5000/catalogs/${CATALOG_DBO_1.fdkId}"))
                .thenReturn(listOf(INFO_MODEL_DBO_1.copy(turtleInformationModel = modelWrongMeta)))

            whenever(valuesMock.catalogUri)
                .thenReturn("http://localhost:5000/catalogs")
            whenever(valuesMock.informationModelUri)
                .thenReturn("http://localhost:5000/informationmodels")

            updateService.updateMetaData()

            argumentCaptor<List<CatalogDBO>>().apply {
                verify(catalogRepository, times(1)).saveAll(capture())
                if (CATALOG_DBO_0 != firstValue.first()) firstValue.first().printTurtleDiff(CATALOG_DBO_0)
                if (CATALOG_DBO_1 != firstValue[1]) firstValue[1].printTurtleDiff(CATALOG_DBO_0)
                assertEquals(listOf(CATALOG_DBO_0, CATALOG_DBO_1), firstValue)
            }

            argumentCaptor<List<InformationModelDBO>>().apply {
                verify(modelRepository, times(1)).saveAll(capture())
                if (INFO_MODEL_DBO_0 != firstValue.first()) firstValue.first().printTurtleDiff(INFO_MODEL_DBO_0)
                if (INFO_MODEL_DBO_1 != firstValue[1]) firstValue[1].printTurtleDiff(INFO_MODEL_DBO_1)
                assertEquals(listOf(INFO_MODEL_DBO_0, INFO_MODEL_DBO_1), firstValue)
            }
        }

    }

    @Nested
    internal inner class UpdateUnionModel {

        @Test
        fun updateUnionModel() {
            whenever(catalogRepository.findAll())
                .thenReturn(listOf(CATALOG_DBO_0, CATALOG_DBO_1))

            updateService.updateUnionModel()

            val expectedWithRecords = responseReader.parseFile("all_catalogs.ttl", "TURTLE")
            val expectedNoRecords = responseReader.parseFile("no_meta_all_catalogs.ttl", "TURTLE")

            val expectedMiscList = listOf(
                MiscellaneousTurtle(
                    id = UNION_ID,
                    isHarvestedSource = false,
                    turtle = gzip(expectedWithRecords.createRDFResponse(JenaType.TURTLE))
                ),
                MiscellaneousTurtle(
                    id = NO_FDK_UNION_ID,
                    isHarvestedSource = false,
                    turtle = gzip(expectedNoRecords.createRDFResponse(JenaType.TURTLE))
                )
            )

            argumentCaptor<Model>().apply {
                verify(fusekiAdapter, times(1)).storeUnionModel(capture())
                assertTrue(firstValue.isIsomorphicWith(expectedWithRecords))
            }

            argumentCaptor<List<MiscellaneousTurtle>>().apply {
                verify(miscRepository, times(1)).saveAll(capture())
                assertEquals(expectedMiscList, firstValue)
            }
        }
    }
}