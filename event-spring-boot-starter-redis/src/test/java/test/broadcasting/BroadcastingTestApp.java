package test.broadcasting;

import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import me.insidezhou.southernquiet.event.CustomApplicationEventRedisRelay;
import me.insidezhou.southernquiet.event.RedisEventAutoConfiguration;
import me.insidezhou.southernquiet.event.RedisTemplateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@SpringBootApplication
@ImportAutoConfiguration({FrameworkAutoConfiguration.class, RedisEventAutoConfiguration.class})
public class BroadcastingTestApp {
    private static Logger log = LoggerFactory.getLogger(BroadcastingTestApp.class);

    public static void main(String[] args) {
        SpringApplication.run(BroadcastingTestApp.class);
    }

    @Bean
    public static CustomApplicationEventRedisRelay customApplicationEventRelay(RedisTemplateBuilder builder, RedisConnectionFactory redisConnectionFactory) {
        return new CustomApplicationEventRedisRelay(builder, redisConnectionFactory);
    }

    @EventListener
    public void testListener(BroadcastingDone broadcastingDone) {
        log.debug("{} {}", broadcastingDone.getClass().getSimpleName(), broadcastingDone.getId());
    }
}
