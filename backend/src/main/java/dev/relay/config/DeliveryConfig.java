package dev.relay.config;

import dev.relay.delivery.BackoffPolicy;
import dev.relay.delivery.DeliveryProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

@Configuration
public class DeliveryConfig {

    @Bean
    public RestClient deliveryRestClient(DeliveryProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(properties.getReadTimeout());
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    @Bean
    public BackoffPolicy backoffPolicy(DeliveryProperties properties) {
        return new BackoffPolicy(
                properties.getInitialBackoff(),
                properties.getMaxBackoff(),
                () -> ThreadLocalRandom.current().nextDouble());
    }

    /**
     * Deliveries run on virtual threads: each in-flight webhook is cheap, so we can
     * hold thousands of blocked HTTP calls without exhausting platform threads.
     */
    @Bean(destroyMethod = "close")
    public ExecutorService deliveryExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
