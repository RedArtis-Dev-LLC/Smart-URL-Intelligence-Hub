package com.smarturl.hub.analytics.amqp;

import com.smarturl.hub.analytics.config.AnalyticsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJavaTypeMapper.TypePrecedence;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RabbitMqConfig {

    private final AnalyticsProperties properties;

    @Bean
    public TopicExchange clickEventsExchange() {
        return new TopicExchange(properties.amqp().clickEventsExchange(), true, false);
    }

    @Bean
    public Queue clickEventsDlq() {
        return QueueBuilder.durable(properties.amqp().clickEventsDlq()).build();
    }

    @Bean
    public Queue clickEventsQueue() {
        return QueueBuilder.durable(properties.amqp().clickEventsQueue())
                .deadLetterExchange("")
                .deadLetterRoutingKey(properties.amqp().clickEventsDlq())
                .build();
    }

    @Bean
    public Binding clickEventsBinding(Queue clickEventsQueue, TopicExchange clickEventsExchange) {
        return BindingBuilder.bind(clickEventsQueue)
                .to(clickEventsExchange)
                .with(properties.amqp().clickEventsRoutingKey());
    }

    @Bean
    public TopicExchange thresholdReachedExchange() {
        return new TopicExchange(properties.amqp().thresholdExchange(), true, false);
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
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        var template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setObservationEnabled(true);
        return template;
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
