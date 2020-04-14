package no.fdk.imcat.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import no.fdk.imcat.service.HarvestAdminClient;
import no.fdk.imcat.service.RDFToModelTransformer;
import no.fdk.imcat.service.RabbitMQListener;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RabbitMQConfig {

    private final HarvestAdminClient harvestAdminClient;
    private final ObjectMapper objectMapper;
    private final RDFToModelTransformer rdfToModelTransformer;

    @Bean
    public RabbitMQListener receiver() {
        return new RabbitMQListener(harvestAdminClient, objectMapper, rdfToModelTransformer);
    }

    @Bean
    public Queue queue() {
        return new AnonymousQueue();
    }

    @Bean
    public Queue sendQueue() {
        return new Queue("harvester.UpdateSearchTrigger", false);
    }

    @Bean
    public Jackson2JsonMessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange("harvests", false, false);
    }

    @Bean
    public Binding binding(TopicExchange topicExchange, Queue queue) {
        return BindingBuilder.bind(queue).to(topicExchange).with("informationmodel.*.HarvestTrigger");
    }

    @Bean
    public Binding sendBinding(TopicExchange topicExchange, Queue sendQueue) {
        return BindingBuilder.bind(sendQueue).to(topicExchange).with("harvester.UpdateSearchTrigger");
    }

    @Bean
    public AmqpTemplate jsonRabbitTemplate(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter());
        return rabbitTemplate;
    }
}
