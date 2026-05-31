package com.smarturl.hub.webhook.amqp;

import com.smarturl.hub.webhook.config.WebhookProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJavaTypeMapper.TypePrecedence;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RabbitMqConfig {

    private final WebhookProperties properties;

    @Bean
    public TopicExchange thresholdExchange() {
        return new TopicExchange(properties.amqp().thresholdExchange(), true, false);
    }

    @Bean
    public Queue thresholdDlq() {
        return QueueBuilder.durable(properties.amqp().thresholdDlq()).build();
    }

    @Bean
    public Queue thresholdQueue() {
        return QueueBuilder.durable(properties.amqp().thresholdQueue())
                .deadLetterExchange("")
                .deadLetterRoutingKey(properties.amqp().thresholdDlq())
                .build();
    }

    @Bean
    public Binding thresholdBinding(Queue thresholdQueue, TopicExchange thresholdExchange) {
        return BindingBuilder.bind(thresholdQueue)
                .to(thresholdExchange)
                .with(properties.amqp().thresholdRoutingKey());
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        var converter = new JacksonJsonMessageConverter();
        var typeMapper = new DefaultJacksonJavaTypeMapper();
        typeMapper.setTypePrecedence(TypePrecedence.INFERRED);
        typeMapper.setTrustedPackages("com.smarturl.hub.*");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        var factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setObservationEnabled(true);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
