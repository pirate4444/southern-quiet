package com.ai.southernquiet.broadcasting;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.RedisSerializer;

import javax.annotation.PostConstruct;

import static com.ai.southernquiet.broadcasting.Broadcaster.CustomApplicationEventChannel;

public class CustomApplicationEventRedisRelay implements ApplicationEventPublisherAware {
    private static Log log = LogFactory.getLog(CustomApplicationEventRedisRelay.class);

    private RedisTemplate redisTemplate;
    private RedisSerializer eventSerializer;
    private RedisSerializer channelSerializer;

    private ApplicationEventPublisher applicationEventPublisher;
    private RedisMessageListenerContainer container;

    public CustomApplicationEventRedisRelay(RedisTemplateBuilder builder, RedisMessageListenerContainer container) {
        this.redisTemplate = builder.getRedisTemplate();
        this.eventSerializer = builder.getEventSerializer();
        this.channelSerializer = builder.getChannelSerializer();

        this.container = container;
    }

    @PostConstruct
    public void postConstruct() {
        container.addMessageListener((message, pattern) -> {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                    "CustomApplicationEventRelay在 %s 频道收到事件，pattern=%s",
                    channelSerializer.deserialize(message.getChannel()),
                    redisTemplate.getStringSerializer().deserialize(pattern)
                ));
            }

            Object event = eventSerializer.deserialize(message.getBody());
            applicationEventPublisher.publishEvent(event);

        }, new ChannelTopic(CustomApplicationEventChannel));
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }
}