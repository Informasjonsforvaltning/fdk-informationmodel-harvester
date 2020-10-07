package no.fdk.fdk_informationmodel_harvester.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("fuseki")
data class FusekiProperties(
    val unionGraphUri: String
)
