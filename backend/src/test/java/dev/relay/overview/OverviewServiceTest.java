package dev.relay.overview;

import dev.relay.api.dto.OverviewResponse;
import dev.relay.delivery.DeliveryAttemptRepository;
import dev.relay.delivery.DeliveryRepository;
import dev.relay.delivery.DeliveryStatus;
import dev.relay.destination.DestinationRepository;
import dev.relay.endpoint.EndpointRepository;
import dev.relay.event.DailyEventCount;
import dev.relay.event.EventRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OverviewServiceTest {

    private final EndpointRepository endpoints = mock(EndpointRepository.class);
    private final DestinationRepository destinations = mock(DestinationRepository.class);
    private final EventRepository events = mock(EventRepository.class);
    private final DeliveryRepository deliveries = mock(DeliveryRepository.class);
    private final DeliveryAttemptRepository attempts = mock(DeliveryAttemptRepository.class);

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-01T10:15:00Z"), ZoneOffset.UTC);
    private final OverviewService service =
            new OverviewService(endpoints, destinations, events, deliveries, attempts, clock);

    private final UUID userId = UUID.randomUUID();

    @Test
    void aggregatesTotalsBreakdownAndRate() {
        when(endpoints.countByUserId(userId)).thenReturn(4L);
        when(endpoints.countByUserIdAndActiveTrue(userId)).thenReturn(3L);
        when(destinations.countByUser(userId)).thenReturn(7L);
        when(events.countByUser(userId)).thenReturn(120L);
        when(deliveries.countByStatusForUser(userId)).thenReturn(List.of(
                new Object[] {DeliveryStatus.DELIVERED, 80L},
                new Object[] {DeliveryStatus.FAILED, 10L},
                new Object[] {DeliveryStatus.RETRYING, 5L},
                new Object[] {DeliveryStatus.PENDING, 5L}));
        when(attempts.averageLatencyForUser(userId)).thenReturn(142.5);
        when(events.countPerDaySince(eq(userId), any())).thenReturn(List.of());

        OverviewResponse response = service.overview(userId);

        assertThat(response.totals().endpoints()).isEqualTo(4L);
        assertThat(response.totals().activeEndpoints()).isEqualTo(3L);
        assertThat(response.totals().destinations()).isEqualTo(7L);
        assertThat(response.totals().events()).isEqualTo(120L);

        assertThat(response.deliveries().delivered()).isEqualTo(80L);
        assertThat(response.deliveries().failed()).isEqualTo(10L);
        assertThat(response.deliveries().retrying()).isEqualTo(5L);
        assertThat(response.deliveries().pending()).isEqualTo(5L);
        assertThat(response.deliveries().total()).isEqualTo(100L);

        assertThat(response.successRate()).isCloseTo(0.8, within(1e-9));
        assertThat(response.averageLatencyMs()).isEqualTo(142.5);
    }

    @Test
    void buildsDenseFourteenDaySeriesWithZeroFill() {
        when(deliveries.countByStatusForUser(userId)).thenReturn(List.of());
        when(attempts.averageLatencyForUser(userId)).thenReturn(null);
        when(events.countPerDaySince(eq(userId), any())).thenReturn(List.of(
                dailyCount(LocalDate.of(2026, 9, 1), 9L),
                dailyCount(LocalDate.of(2026, 8, 30), 4L)));

        OverviewResponse response = service.overview(userId);

        assertThat(response.eventsPerDay()).hasSize(14);
        assertThat(response.eventsPerDay().get(0).date()).isEqualTo(LocalDate.of(2026, 8, 19));
        assertThat(response.eventsPerDay().get(13).date()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(response.eventsPerDay().get(13).count()).isEqualTo(9L);
        assertThat(response.eventsPerDay().get(11).count()).isEqualTo(4L);
        assertThat(response.eventsPerDay().get(12).count()).isZero();
        assertThat(response.successRate()).isZero();
        assertThat(response.averageLatencyMs()).isNull();
    }

    private static DailyEventCount dailyCount(LocalDate day, long total) {
        return new DailyEventCount() {
            @Override
            public LocalDate getDay() {
                return day;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }
}
