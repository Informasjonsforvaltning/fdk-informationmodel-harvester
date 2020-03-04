package no.fdk.imcat.utils

import org.slf4j.LoggerFactory
import org.testcontainers.Testcontainers
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy
import java.time.Duration

abstract class ElasticsearchTestContainer {
    companion object {

        private val logger = LoggerFactory.getLogger(ElasticsearchTestContainer::class.java)
        var elasticContainer: KGenericContainer

        init {
            Testcontainers.exposeHostPorts(LOCAL_SERVER_PORT)

            elasticContainer = KGenericContainer("docker.elastic.co/elasticsearch/elasticsearch:5.6.9")
                    .withLogConsumer(Slf4jLogConsumer(logger).withPrefix("elastic"))
                    .withEnv(ELASTIC_ENV_VALUES)
                    .withExposedPorts(ELASTIC_PORT, ELASTIC_TCP_PORT)
                    .waitingFor(HttpWaitStrategy()
                            .forPort(ELASTIC_PORT)
                            .forPath("/_cluster/health?pretty=true")
                            .forStatusCode(200)
                            .withStartupTimeout(Duration.ofMinutes(1)))
            elasticContainer.start()
        }
    }
}
