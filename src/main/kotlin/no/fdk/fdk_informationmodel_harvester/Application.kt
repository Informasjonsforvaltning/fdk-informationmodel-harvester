package no.fdk.fdk_informationmodel_harvester

import no.fdk.fdk_informationmodel_harvester.configuration.ApplicationProperties
import no.fdk.fdk_informationmodel_harvester.configuration.FusekiProperties
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties

@SpringBootApplication
@EnableConfigurationProperties(ApplicationProperties::class, FusekiProperties::class, OAuth2ResourceServerProperties::class)
open class Application

fun main(args: Array<String>) {
    SpringApplication.run(Application::class.java, *args)
}
