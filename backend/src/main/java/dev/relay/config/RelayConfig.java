package dev.relay.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class RelayConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
