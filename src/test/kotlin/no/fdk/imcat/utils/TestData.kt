package no.fdk.imcat.utils

import no.fdk.imcat.utils.ApiTestContainer.Companion.TEST_API

const val API_PORT = 8080
const val LOCAL_SERVER_PORT = 5000

const val ELASTIC_PORT = 9200
const val ELASTIC_TCP_PORT = 9300
const val ELASTIC_NETWORK_NAME = "elasticsearch5"
const val ELASTIC_CLUSERNAME = "elasticsearch"
const val ELASTIC_CLUSTERNODES = "$ELASTIC_NETWORK_NAME:$ELASTIC_TCP_PORT"

const val RABBIT_USERNAME = "admin"
const val RABBIT_PASSWORD = "admin"
const val FDK_REFERENCE_DATA_URL = ""
const val FDK_ORGANIZATION_CATALOGUE_URL = ""

const val WIREMOCK_TEST_HOST = "http://host.testcontainers.internal:$LOCAL_SERVER_PORT"

val API_ENV_VALUES: Map<String, String> = mapOf(
        "SPRING_PROFILES_ACTIVE" to "test",
        "WIREMOCK_TEST_HOST" to WIREMOCK_TEST_HOST,
        "FDK_ES_CLUSTERNODES" to ELASTIC_CLUSTERNODES,
        "FDK_ES_CLUSTERNAME" to ELASTIC_CLUSERNAME,
        "RABBIT_USERNAME" to RABBIT_USERNAME,
        "RABBIT_PASSWORD" to RABBIT_PASSWORD,
        "FDK_REFERENCE_DATA_URL" to FDK_REFERENCE_DATA_URL,
        "FDK_ORGANIZATION_CATALOGUE_URL" to FDK_ORGANIZATION_CATALOGUE_URL
)

val ELASTIC_ENV_VALUES: Map<String, String> = mapOf(
        "cluster.name" to ELASTIC_CLUSERNAME,
        "xpack.security.enabled" to "false",
        "xpack.monitoring.enabled" to "false"
)

fun getApiAddress(endpoint: String): String {
    return "http://${TEST_API.getContainerIpAddress()}:${TEST_API.getMappedPort(API_PORT)}$endpoint"
}