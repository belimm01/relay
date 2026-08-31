package dev.relay.config;

import dev.relay.delivery.DeliveryProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(DeliveryProperties.class)
public class RelayConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
