package no.fdk.fdk_informationmodel_harvester.service

import no.fdk.fdk_informationmodel_harvester.model.FdkIdAndUri
import no.fdk.fdk_informationmodel_harvester.model.HarvestReport
import no.fdk.fdk_informationmodel_harvester.model.InformationModelMeta
import no.fdk.fdk_informationmodel_harvester.rabbit.RabbitMQPublisher
import no.fdk.fdk_informationmodel_harvester.repository.InformationModelRepository
import no.fdk.fdk_informationmodel_harvester.utils.*
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.web.server.ResponseStatusException

@Tag("unit")
class InformationModelServiceTest {
    private val repository: InformationModelRepository = mock()
    private val publisher: RabbitMQPublisher = mock()
    private val turtleService: TurtleService = mock()
    private val modelService = InformationModelService(repository, publisher, turtleService)

    private val responseReader = TestResponseReader()

    @Nested
    internal inner class AllCatalogs {

        @Test
        fun responseIsometricWithEmptyModelForEmptyDB() {
            whenever(turtleService.findUnionModel(withRecords = true))
                .thenReturn(null)
            whenever(turtleService.findUnionModel(withRecords = false))
                .thenReturn(null)

            val expected = responseReader.parseResponse("", "TURTLE")

            val responseTurtle = modelService.getAll(Lang.TURTLE, true)
            val responseJsonLD = modelService.getAll(Lang.JSONLD, false)

            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseTurtle, "TURTLE")))
            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseJsonLD, "JSON-LD")))
        }

        @Test
        fun getAllHandlesTurtleAndOtherRDF() {
            whenever(turtleService.findUnionModel(true))
                .thenReturn(javaClass.classLoader.getResource("all_catalogs.ttl")!!.readText())

            val expected = responseReader.parseFile("all_catalogs.ttl", "TURTLE")

            val responseTurtle = modelService.getAll(Lang.TURTLE, true)
            val responseN3 = modelService.getAll(Lang.N3, true)
            val responseNTriples = modelService.getAll(Lang.NTRIPLES, true)

            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseTurtle, "TURTLE")))
            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseN3, "N3")))
            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseNTriples, "N-TRIPLES")))
        }

        @Test
        fun getAllHarvestedHandlesTurtleAndOtherRDF() {
            whenever(turtleService.findUnionModel(false))
                .thenReturn(javaClass.classLoader.getResource("no_meta_all_catalogs.ttl")!!.readText())

            val expected = responseReader.parseFile("no_meta_all_catalogs.ttl", "TURTLE")

            val responseTurtle = modelService.getAll(Lang.TURTLE, false)
            val responseN3 = modelService.getAll(Lang.N3, false)
            val responseNTriples = modelService.getAll(Lang.NTRIPLES, false)

            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseTurtle, "TURTLE")))
            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseN3, "N3")))
            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseNTriples, "N-TRIPLES")))
        }

    }

    @Nested
    internal inner class CatalogById {

        @Test
        fun responseIsNullWhenNoCatalogIsFound() {
            whenever(turtleService.findInformationModel("123", true))
                .thenReturn(null)

            val response = modelService.getCatalogById("123", Lang.TURTLE, true)

            assertNull(response)
        }

        @Test
        fun responseIsIsomorphicWithExpectedModel() {
            whenever(turtleService.findCatalog(CATALOG_ID_0, withRecords = true))
                .thenReturn(javaClass.classLoader.getResource("catalog_0.ttl")!!.readText())
            whenever(turtleService.findCatalog(CATALOG_ID_0, withRecords = false))
                .thenReturn(javaClass.classLoader.getResource("no_meta_catalog_0.ttl")!!.readText())

            val responseTurtle = modelService.getCatalogById(CATALOG_ID_0, Lang.TURTLE, true)
            val responseJsonRDF = modelService.getCatalogById(CATALOG_ID_0, Lang.RDFJSON, false)

            val expectedWithMeta = responseReader.parseFile("catalog_0.ttl", "TURTLE")
            val expectedNoMeta = responseReader.parseFile("no_meta_catalog_0.ttl", "TURTLE")

            assertTrue(expectedWithMeta.isIsomorphicWith(responseReader.parseResponse(responseTurtle!!, "TURTLE")))
            assertTrue(expectedNoMeta.isIsomorphicWith(responseReader.parseResponse(responseJsonRDF!!, "RDF/JSON")))
        }

    }

    @Nested
    internal inner class InformationModelById {

        @Test
        fun responseIsNullWhenNoModelIsFound() {
            whenever(turtleService.findInformationModel("123", true))
                .thenReturn(null)

            val response = modelService.getInformationModelById("123", Lang.TURTLE, true)

            assertNull(response)
        }

        @Test
        fun responseIsIsomorphicWithExpectedModel() {
            whenever(turtleService.findInformationModel(INFO_MODEL_ID_0, true))
                .thenReturn(javaClass.classLoader.getResource("model_0.ttl")!!.readText())
            whenever(turtleService.findInformationModel(INFO_MODEL_ID_0, false))
                .thenReturn(javaClass.classLoader.getResource("no_meta_model_0.ttl")!!.readText())

            val responseTurtle = modelService.getInformationModelById(INFO_MODEL_ID_0, Lang.TURTLE, false)
            val responseRDFXML = modelService.getInformationModelById(INFO_MODEL_ID_0, Lang.RDFXML, true)

            val expectedWithMeta = responseReader.parseFile("model_0.ttl", "TURTLE")
            val expectedNoMeta = responseReader.parseFile("no_meta_model_0.ttl", "TURTLE")

            assertTrue(expectedNoMeta.isIsomorphicWith(responseReader.parseResponse(responseTurtle!!, "TURTLE")))
            assertTrue(expectedWithMeta.isIsomorphicWith(responseReader.parseResponse(responseRDFXML!!, "RDF/XML")))
        }

    }

    @Nested
    internal inner class RemoveInformationModelById {

        @Test
        fun throwsResponseStatusExceptionWhenNoMetaFoundInDB() {
            whenever(repository.findAllByFdkId("123"))
                .thenReturn(emptyList())

            assertThrows<ResponseStatusException> { modelService.removeInformationModel("123") }
        }

        @Test
        fun throwsExceptionWhenNoNonRemovedMetaFoundInDB() {
            whenever(repository.findAllByFdkId(INFO_MODEL_ID_0))
                .thenReturn(listOf(INFO_MODEL_DBO_0.copy(removed = true)))

            assertThrows<ResponseStatusException> { modelService.removeInformationModel(INFO_MODEL_ID_0) }
        }

        @Test
        fun updatesMetaAndSendsRabbitReportWhenMetaIsFound() {
            whenever(repository.findAllByFdkId(INFO_MODEL_DBO_0.fdkId))
                .thenReturn(listOf(INFO_MODEL_DBO_0))

            modelService.removeInformationModel(INFO_MODEL_DBO_0.fdkId)

            argumentCaptor<List<InformationModelMeta>>().apply {
                verify(repository, times(1)).saveAll(capture())
                assertEquals(listOf(INFO_MODEL_DBO_0.copy(removed = true)), firstValue)
            }

            val expectedReport = HarvestReport(
                id = "manual-delete-$INFO_MODEL_ID_0",
                url = INFO_MODEL_DBO_0.uri,
                harvestError = false,
                startTime = "startTime",
                endTime = "endTime",
                removedResources = listOf(FdkIdAndUri(INFO_MODEL_DBO_0.fdkId, INFO_MODEL_DBO_0.uri))
            )
            argumentCaptor<List<HarvestReport>>().apply {
                verify(publisher, times(1)).send(capture())

                assertEquals(
                    listOf(expectedReport.copy(
                        startTime = firstValue.first().startTime,
                        endTime = firstValue.first().endTime
                    )),
                    firstValue
                )
            }
        }

    }

}
