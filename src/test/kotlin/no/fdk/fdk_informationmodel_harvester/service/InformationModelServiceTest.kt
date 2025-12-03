package no.fdk.fdk_informationmodel_harvester.service

import no.fdk.fdk_informationmodel_harvester.model.DuplicateIRI
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
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.web.server.ResponseStatusException
import java.util.*

@Tag("unit")
class InformationModelServiceTest {
    private val repository: InformationModelRepository = mock()
    private val publisher: RabbitMQPublisher = mock()
    private val turtleService: TurtleService = mock()
    private val modelService = InformationModelService(repository, publisher, turtleService)

    private val responseReader = TestResponseReader()

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

    @Nested
    internal inner class RemoveDuplicates {

        @Test
        fun throwsExceptionWhenRemoveIRINotFoundInDB() {
            whenever(repository.findById("https://123.no"))
                .thenReturn(Optional.empty())
            whenever(repository.findById(INFO_MODEL_DBO_1.uri))
                .thenReturn(Optional.of(INFO_MODEL_DBO_1))

            val duplicateIRI = DuplicateIRI(
                iriToRemove = "https://123.no",
                iriToRetain = INFO_MODEL_DBO_1.uri
            )
            assertThrows<ResponseStatusException> { modelService.removeDuplicates(listOf(duplicateIRI)) }
        }

        @Test
        fun createsNewMetaWhenRetainIRINotFoundInDB() {
            whenever(repository.findById(INFO_MODEL_DBO_0.uri))
                .thenReturn(Optional.of(INFO_MODEL_DBO_0))
            whenever(repository.findById(INFO_MODEL_DBO_1.uri))
                .thenReturn(Optional.empty())

            val duplicateIRI = DuplicateIRI(
                iriToRemove = INFO_MODEL_DBO_0.uri,
                iriToRetain = INFO_MODEL_DBO_1.uri
            )
            modelService.removeDuplicates(listOf(duplicateIRI))

            argumentCaptor<List<InformationModelMeta>>().apply {
                verify(repository, times(1)).saveAll(capture())
                assertEquals(listOf(INFO_MODEL_DBO_0.copy(removed = true), INFO_MODEL_DBO_0.copy(uri = INFO_MODEL_DBO_1.uri)), firstValue)
            }

            verify(publisher, times(0)).send(any())
        }

        @Test
        fun sendsRabbitReportWithRetainFdkIdWhenKeepingRemoveFdkId() {
            whenever(repository.findById(INFO_MODEL_DBO_0.uri))
                .thenReturn(Optional.of(INFO_MODEL_DBO_0))
            whenever(repository.findById(INFO_MODEL_DBO_1.uri))
                .thenReturn(Optional.of(INFO_MODEL_DBO_1))

            val duplicateIRI = DuplicateIRI(
                iriToRemove = INFO_MODEL_DBO_0.uri,
                iriToRetain = INFO_MODEL_DBO_1.uri
            )
            modelService.removeDuplicates(listOf(duplicateIRI))

            argumentCaptor<List<InformationModelMeta>>().apply {
                verify(repository, times(1)).saveAll(capture())
                assertEquals(listOf(
                    INFO_MODEL_DBO_0.copy(removed = true),
                    INFO_MODEL_DBO_0.copy(uri = INFO_MODEL_DBO_1.uri, isPartOf = INFO_MODEL_DBO_1.isPartOf)
                ), firstValue)
            }

            val expectedReport = HarvestReport(
                id = "duplicate-delete",
                url = "https://fellesdatakatalog.digdir.no/duplicates",
                harvestError = false,
                startTime = "startTime",
                endTime = "endTime",
                removedResources = listOf(FdkIdAndUri(INFO_MODEL_DBO_1.fdkId, INFO_MODEL_DBO_1.uri))
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

        @Test
        fun sendsRabbitReportWithRemoveFdkIdWhenNotKeepingRemoveFdkId() {
            whenever(repository.findById(INFO_MODEL_DBO_0.uri))
                .thenReturn(Optional.of(INFO_MODEL_DBO_0))
            whenever(repository.findById(INFO_MODEL_DBO_1.uri))
                .thenReturn(Optional.of(INFO_MODEL_DBO_1))

            val duplicateIRI = DuplicateIRI(
                iriToRemove = INFO_MODEL_DBO_1.uri,
                iriToRetain = INFO_MODEL_DBO_0.uri,
                keepRemovedFdkId = false
            )
            modelService.removeDuplicates(listOf(duplicateIRI))

            argumentCaptor<List<InformationModelMeta>>().apply {
                verify(repository, times(1)).saveAll(capture())
                assertEquals(listOf(
                    INFO_MODEL_DBO_1.copy(removed = true),
                    INFO_MODEL_DBO_0
                ), firstValue)
            }

            val expectedReport = HarvestReport(
                id = "duplicate-delete",
                url = "https://fellesdatakatalog.digdir.no/duplicates",
                harvestError = false,
                startTime = "startTime",
                endTime = "endTime",
                removedResources = listOf(FdkIdAndUri(INFO_MODEL_DBO_1.fdkId, INFO_MODEL_DBO_1.uri))
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

        @Test
        fun throwsExceptionWhenTryingToReportAlreadyRemovedAsRemoved() {
            whenever(repository.findById(INFO_MODEL_DBO_0.uri))
                .thenReturn(Optional.of(INFO_MODEL_DBO_0.copy(removed = true)))
            whenever(repository.findById(INFO_MODEL_DBO_1.uri))
                .thenReturn(Optional.of(INFO_MODEL_DBO_1))

            val duplicateIRI = DuplicateIRI(
                iriToRemove = INFO_MODEL_DBO_0.uri,
                iriToRetain = INFO_MODEL_DBO_1.uri,
                keepRemovedFdkId = false
            )

            assertThrows<ResponseStatusException> { modelService.removeDuplicates(listOf(duplicateIRI)) }

            whenever(repository.findById(INFO_MODEL_DBO_0.uri))
                .thenReturn(Optional.of(INFO_MODEL_DBO_0))
            whenever(repository.findById(INFO_MODEL_DBO_1.uri))
                .thenReturn(Optional.of(INFO_MODEL_DBO_1.copy(removed = true)))

            assertThrows<ResponseStatusException> { modelService.removeDuplicates(listOf(duplicateIRI.copy(keepRemovedFdkId = true))) }
        }

    }

}
