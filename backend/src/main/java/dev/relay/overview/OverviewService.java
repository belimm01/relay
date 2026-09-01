package dev.relay.overview;

import dev.relay.api.dto.OverviewResponse;
import dev.relay.delivery.DeliveryAttemptRepository;
import dev.relay.delivery.DeliveryRepository;
import dev.relay.delivery.DeliveryStatus;
import dev.relay.destination.DestinationRepository;
import dev.relay.endpoint.EndpointRepository;
import dev.relay.event.DailyEventCount;
import dev.relay.event.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OverviewService {

    private static final int WINDOW_DAYS = 14;

    private final EndpointRepository endpoints;
    private final DestinationRepository destinations;
    private final EventRepository events;
    private final DeliveryRepository deliveries;
    private final DeliveryAttemptRepository attempts;
    private final Clock clock;

    public OverviewService(EndpointRepository endpoints,
                           DestinationRepository destinations,
                           EventRepository events,
                           DeliveryRepository deliveries,
                           DeliveryAttemptRepository attempts,
                           Clock clock) {
        this.endpoints = endpoints;
        this.destinations = destinations;
        this.events = events;
        this.deliveries = deliveries;
        this.attempts = attempts;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public OverviewResponse overview(UUID userId) {
        OverviewResponse.Totals totals = new OverviewResponse.Totals(
                endpoints.countByUserId(userId),
                endpoints.countByUserIdAndActiveTrue(userId),
                destinations.countByUser(userId),
                events.countByUser(userId));

        Map<DeliveryStatus, Long> byStatus = new EnumMap<>(DeliveryStatus.class);
        for (Object[] row : deliveries.countByStatusForUser(userId)) {
            byStatus.put((DeliveryStatus) row[0], (Long) row[1]);
        }
        long delivered = byStatus.getOrDefault(DeliveryStatus.DELIVERED, 0L);
        long pending = byStatus.getOrDefault(DeliveryStatus.PENDING, 0L);
        long retrying = byStatus.getOrDefault(DeliveryStatus.RETRYING, 0L);
        long failed = byStatus.getOrDefault(DeliveryStatus.FAILED, 0L);
        long total = delivered + pending + retrying + failed;

        OverviewResponse.DeliveryBreakdown breakdown =
                new OverviewResponse.DeliveryBreakdown(delivered, pending, retrying, failed, total);
        double successRate = total == 0 ? 0.0 : (double) delivered / total;

        Double averageLatency = attempts.averageLatencyForUser(userId);

        return new OverviewResponse(totals, breakdown, successRate, averageLatency, eventsPerDay(userId));
    }

    private List<OverviewResponse.DailyCount> eventsPerDay(UUID userId) {
        LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
        LocalDate start = today.minusDays(WINDOW_DAYS - 1L);
        Instant since = start.atStartOfDay(ZoneOffset.UTC).toInstant();

        Map<LocalDate, Long> counts = new HashMap<>();
        for (DailyEventCount row : events.countPerDaySince(userId, since)) {
            counts.put(row.getDay(), row.getTotal());
        }

        List<OverviewResponse.DailyCount> series = new ArrayList<>(WINDOW_DAYS);
        for (int i = 0; i < WINDOW_DAYS; i++) {
            LocalDate day = start.plusDays(i);
            series.add(new OverviewResponse.DailyCount(day, counts.getOrDefault(day, 0L)));
        }
        return series;
    }
}
