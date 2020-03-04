package no.fdk.imcat.utils

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext

class PropertyOverrideContextInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

    companion object {
        internal val mappedPort = ElasticsearchTestContainer.elasticContainer.getMappedPort(ELASTIC_TCP_PORT)
    }

    override fun initialize(ctx: ConfigurableApplicationContext) {
        TestPropertyValues.of("spring.data.elasticsearch.clusterNodes=localhost:$mappedPort").applyTo(ctx)
    }
}