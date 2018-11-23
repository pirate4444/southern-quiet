package com.ai.southernquiet.notification.driver;

import com.ai.southernquiet.notification.NotificationPublisher;
import com.ai.southernquiet.notification.NotificationSource;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import static com.ai.southernquiet.notification.AmqpNotificationAutoConfiguration.NAME_PREFIX;

public class AmqpNotificationPublisher<N extends Serializable> implements NotificationPublisher<N> {
    private RabbitTemplate rabbitTemplate;
    private AmqpAdmin amqpAdmin;
    private MessageConverter messageConverter;

    private Set<String> declaredExchanges = new HashSet<>();

    public AmqpNotificationPublisher(
        ConnectionFactory connectionFactory,
        MessageConverter messageConverter,
        AmqpAdmin amqpAdmin
    ) {
        this.amqpAdmin = amqpAdmin;
        this.messageConverter = messageConverter;

        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        this.rabbitTemplate = rabbitTemplate;
    }

    public RabbitTemplate getRabbitTemplate() {
        return rabbitTemplate;
    }

    public MessageConverter getMessageConverter() {
        return messageConverter;
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    @Override
    public void publish(N notification) {
        String source = getNotificationSource((Class<N>) notification.getClass());
        String exchange = getExchange(source);
        String routing = getRouting(source);

        declareExchange(exchange);

        rabbitTemplate.convertAndSend(exchange, routing, notification);
    }

    @SuppressWarnings("ConstantConditions")
    public String getNotificationSource(Class<N> cls) {
        NotificationSource annotation = AnnotationUtils.getAnnotation(cls, NotificationSource.class);
        return null == annotation || StringUtils.isEmpty(annotation.source()) ? cls.getName() : annotation.source();
    }

    public String getExchange(String source) {
        return NAME_PREFIX + "EXCHANGE." + source;
    }

    public String getRouting(String source) {
        return NAME_PREFIX + source;
    }

    public Exchange declareExchange(String exchangeName) {
        if (declaredExchanges.contains(exchangeName)) return new FanoutExchange(exchangeName);

        Exchange exchange = new FanoutExchange(exchangeName);
        amqpAdmin.declareExchange(exchange);

        declaredExchanges.add(exchangeName);

        return exchange;
    }

    public Exchange declareExchange(Class<N> cls) {
        return declareExchange(getExchange(getNotificationSource(cls)));
    }
}