package no.fdk.fdk_informationmodel_harvester.utils

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import no.fdk.fdk_informationmodel_harvester.model.CatalogDBO
import no.fdk.fdk_informationmodel_harvester.model.InformationModelDBO
import no.fdk.fdk_informationmodel_harvester.model.MiscellaneousTurtle
import no.fdk.fdk_informationmodel_harvester.rdf.JenaType
import no.fdk.fdk_informationmodel_harvester.rdf.createRDFResponse
import no.fdk.fdk_informationmodel_harvester.rdf.parseRDFResponse
import no.fdk.fdk_informationmodel_harvester.service.ungzip
import no.fdk.fdk_informationmodel_harvester.utils.ApiTestContext.Companion.mongoContainer
import org.apache.jena.rdf.model.Model
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.pojo.PojoCodecProvider
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.net.URL
import org.springframework.http.HttpStatus
import java.net.HttpURLConnection

private val logger = LoggerFactory.getLogger(ApiTestContext::class.java)

fun apiGet(endpoint: String, acceptHeader: String?): Map<String,Any> {

    return try {
        val connection = URL("$API_TEST_URI$endpoint").openConnection() as HttpURLConnection
        if(acceptHeader != null) connection.setRequestProperty("Accept", acceptHeader)
        connection.connect()

        if(isOK(connection.responseCode)) {
            val responseBody = connection.inputStream.bufferedReader().use(BufferedReader::readText)
            mapOf(
                "body"   to responseBody,
                "header" to connection.headerFields.toString(),
                "status" to connection.responseCode)
        } else {
            mapOf(
                "status" to connection.responseCode,
                "header" to " ",
                "body"   to " "
            )
        }
    } catch (e: Exception) {
        mapOf(
            "status" to e.toString(),
            "header" to " ",
            "body"   to " "
        )
    }
}

fun apiPost(endpoint: String, headers: Map<String, String>): Map<String,Any> {

    return try {
        val connection = URL("$API_TEST_URI$endpoint").openConnection() as HttpURLConnection
        headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
        connection.requestMethod = "POST"
        connection.connect()

        if(isOK(connection.responseCode)) {
            val responseBody = connection.inputStream.bufferedReader().use(BufferedReader::readText)
            mapOf(
                "body"   to responseBody,
                "header" to connection.headerFields.toString(),
                "status" to connection.responseCode)
        } else {
            mapOf(
                "status" to connection.responseCode,
                "header" to " ",
                "body"   to " "
            )
        }
    } catch (e: Exception) {
        mapOf(
            "status" to e.toString(),
            "header" to " ",
            "body"   to " "
        )
    }
}

private fun isOK(response: Int?): Boolean =
    if(response == null) false
    else HttpStatus.resolve(response)?.is2xxSuccessful == true

fun populateDB() {
    val connectionString = ConnectionString("mongodb://${MONGO_USER}:${MONGO_PASSWORD}@localhost:${mongoContainer.getMappedPort(MONGO_PORT)}/$MONGO_COLLECTION?authSource=admin&authMechanism=SCRAM-SHA-1")
    val pojoCodecRegistry = CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build()))

    val client: MongoClient = MongoClients.create(connectionString)
    val mongoDatabase = client.getDatabase(MONGO_COLLECTION).withCodecRegistry(pojoCodecRegistry)

    val miscCollection = mongoDatabase.getCollection("misc")
    miscCollection.insertMany(miscDBPopulation())

    val catalogCollection = mongoDatabase.getCollection("catalog")
    catalogCollection.insertMany(catalogDBPopulation())

    val serviceCollection = mongoDatabase.getCollection("informationmodel")
    serviceCollection.insertMany(serviceDBPopulation())

    client.close()
}

fun MiscellaneousTurtle.printTurtleDiff(expected: MiscellaneousTurtle) {
    val actualModel = parseRDFResponse(ungzip(turtle), JenaType.TURTLE, null)
    val expectedModel = parseRDFResponse(ungzip(expected.turtle), JenaType.TURTLE, null)

    val actualDiff = actualModel!!.difference(expectedModel).createRDFResponse(JenaType.TURTLE)
    val expectedDiff = expectedModel!!.difference(actualModel).createRDFResponse(JenaType.TURTLE)

    if (actualDiff.isNotEmpty()) {
        logger.error("non expected misc nodes:")
        logger.error(actualDiff)
    }
    if (expectedDiff.isNotEmpty()) {
        logger.error("missing misc nodes:")
        logger.error(expectedDiff)
    }
}

fun CatalogDBO.printTurtleDiff(expected: CatalogDBO) {
    val actualModel = parseRDFResponse(ungzip(turtleCatalog), JenaType.TURTLE, null)
    val expectedModel = parseRDFResponse(ungzip(expected.turtleCatalog), JenaType.TURTLE, null)

    checkIfIsomorphicAndPrintDiff(actualModel!!, expectedModel!!, "CatalogDBO")
}

fun InformationModelDBO.printTurtleDiff(expected: InformationModelDBO) {
    val actualModel = parseRDFResponse(ungzip(turtleInformationModel), JenaType.TURTLE, null)
    val expectedModel = parseRDFResponse(ungzip(expected.turtleInformationModel), JenaType.TURTLE, null)

    checkIfIsomorphicAndPrintDiff(actualModel!!, expectedModel!!, "InformationModelDBO")
}

fun checkIfIsomorphicAndPrintDiff(actual: Model, expected: Model, name: String): Boolean {
    val isIsomorphic = actual.isIsomorphicWith(expected)

    if (!isIsomorphic) {
        val missing = actual.difference(expected).createRDFResponse(JenaType.TURTLE)
        val nonExpected = expected.difference(actual).createRDFResponse(JenaType.TURTLE)

        if (missing.isNotEmpty()) {
            logger.error("missing nodes in $name:")
            logger.error(missing)
        }
        if (nonExpected.isNotEmpty()) {
            logger.error("non expected nodes in $name:")
            logger.error(nonExpected)
        }
    }
    return isIsomorphic
}
