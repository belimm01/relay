package dev.relay.delivery;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class DeliveryQueueMetrics {

    static final String QUEUE_METRIC = "relay.deliveries.queue";
    static final String STATUS_TAG = "status";

    public DeliveryQueueMetrics(DeliveryRepository deliveries, MeterRegistry meters) {
        for (DeliveryStatus status : DeliveryStatus.values()) {
            Gauge.builder(QUEUE_METRIC, deliveries, repository -> repository.countByStatus(status))
                    .tag(STATUS_TAG, status.name().toLowerCase(Locale.ROOT))
                    .register(meters);
        }
    }
}
