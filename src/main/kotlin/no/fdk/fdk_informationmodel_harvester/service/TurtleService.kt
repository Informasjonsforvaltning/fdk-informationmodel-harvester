package no.fdk.fdk_informationmodel_harvester.service

import no.fdk.fdk_informationmodel_harvester.repository.TurtleRepository
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream


private const val NO_RECORDS_ID_PREFIX = "no-records-"
private const val CATALOG_ID_PREFIX = "catalog-"
private const val INFORMATION_MODEL_ID_PREFIX = "informationmodel-"
private const val UNION_ID = "information-model-catalogs-union-graph"

@Service
class TurtleService (private val turtleRepository: TurtleRepository) {

    fun saveOne(filename: String, turtle: String) =
        turtleRepository.saveFile(content = gzip(turtle), filename)

    fun findOne(filename: String): String? =
        turtleRepository.readFileContent(filename)
            ?.let { ungzip(it) }

    fun saveCatalog(fdkId: String, turtle: String, withRecords: Boolean)  =
        saveOne(filename = filenameCatalog(fdkId, withRecords), turtle)

    fun findCatalog(fdkId: String, withRecords: Boolean): String? =
        findOne(filenameCatalog(fdkId, withRecords))

    fun saveInformationModel(fdkId: String, turtle: String, withRecords: Boolean) =
        saveOne(filename = filenameInformationModel(fdkId, withRecords), turtle)

    fun findInformationModel(fdkId: String, withRecords: Boolean): String? =
        findOne(filenameInformationModel(fdkId, withRecords))

    fun saveUnionModel(turtle: String, withRecords: Boolean) =
        saveOne(filename = filenameUnion(withRecords), turtle)

    fun findUnionModel(withRecords: Boolean): String? =
        findOne(filenameUnion(withRecords))

    private fun filenameCatalog(fdkId: String, withFDKRecords: Boolean): String =
        "$CATALOG_ID_PREFIX${if (withFDKRecords) "" else NO_RECORDS_ID_PREFIX}$fdkId"

    private fun filenameInformationModel(fdkId: String, withFDKRecords: Boolean): String =
        "$INFORMATION_MODEL_ID_PREFIX${if (withFDKRecords) "" else NO_RECORDS_ID_PREFIX}$fdkId"

    private fun filenameUnion(withFDKRecords: Boolean): String =
        "${if (withFDKRecords) "" else NO_RECORDS_ID_PREFIX}$UNION_ID"

    private fun gzip(content: String): String {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).bufferedWriter(Charsets.UTF_8).use { it.write(content) }
        return Base64.getEncoder().encodeToString(bos.toByteArray())
    }

    private fun ungzip(base64Content: String): String {
        val content = Base64.getDecoder().decode(base64Content)
        return GZIPInputStream(content.inputStream())
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
    }
}