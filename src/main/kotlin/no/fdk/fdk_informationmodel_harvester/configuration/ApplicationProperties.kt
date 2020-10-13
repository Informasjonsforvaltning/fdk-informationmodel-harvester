package no.fdk.fdk_informationmodel_harvester.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("application")
data class ApplicationProperties(
    val informationModelUri: String,
    val catalogUri: String,
    val harvestAdminRootUrl: String
)