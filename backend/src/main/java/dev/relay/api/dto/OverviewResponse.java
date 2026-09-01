package dev.relay.api.dto;

import java.time.LocalDate;
import java.util.List;

public record OverviewResponse(
        Totals totals,
        DeliveryBreakdown deliveries,
        double successRate,
        Double averageLatencyMs,
        List<DailyCount> eventsPerDay) {

    public record Totals(
            long endpoints,
            long activeEndpoints,
            long destinations,
            long events) {
    }

    public record DeliveryBreakdown(
            long delivered,
            long pending,
            long retrying,
            long failed,
            long total) {
    }

    public record DailyCount(LocalDate date, long count) {
    }
}
