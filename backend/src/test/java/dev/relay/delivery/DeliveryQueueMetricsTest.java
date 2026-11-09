package dev.relay.delivery;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeliveryQueueMetricsTest {

    private final DeliveryRepository deliveries = mock(DeliveryRepository.class);
    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();

    @Test
    void registersOneGaugePerStatusReflectingCurrentCounts() {
        when(deliveries.countByStatus(DeliveryStatus.PENDING)).thenReturn(3L);
        when(deliveries.countByStatus(DeliveryStatus.RETRYING)).thenReturn(2L);
        when(deliveries.countByStatus(DeliveryStatus.DELIVERED)).thenReturn(10L);
        when(deliveries.countByStatus(DeliveryStatus.FAILED)).thenReturn(1L);

        new DeliveryQueueMetrics(deliveries, meters);

        assertThat(queueDepth("pending")).isEqualTo(3.0);
        assertThat(queueDepth("retrying")).isEqualTo(2.0);
        assertThat(queueDepth("delivered")).isEqualTo(10.0);
        assertThat(queueDepth("failed")).isEqualTo(1.0);
    }

    @Test
    void reflectsLaterCountChangesOnEachObservation() {
        when(deliveries.countByStatus(DeliveryStatus.PENDING)).thenReturn(0L);

        new DeliveryQueueMetrics(deliveries, meters);
        assertThat(queueDepth("pending")).isZero();

        when(deliveries.countByStatus(DeliveryStatus.PENDING)).thenReturn(7L);
        assertThat(queueDepth("pending")).isEqualTo(7.0);
    }

    private double queueDepth(String status) {
        return meters.get(DeliveryQueueMetrics.QUEUE_METRIC)
                .tag(DeliveryQueueMetrics.STATUS_TAG, status)
                .gauge()
                .value();
    }
}
