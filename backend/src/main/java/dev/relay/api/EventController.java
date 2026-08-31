package dev.relay.api;

import dev.relay.api.dto.EventDetailResponse;
import dev.relay.api.dto.EventSummary;
import dev.relay.api.dto.PageResponse;
import dev.relay.delivery.ReplayService;
import dev.relay.event.EventService;
import dev.relay.security.CurrentUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
public class EventController {

    private static final int MAX_PAGE_SIZE = 200;

    private final EventService events;
    private final ReplayService replayService;
    private final CurrentUser currentUser;

    public EventController(EventService events, ReplayService replayService, CurrentUser currentUser) {
        this.events = events;
        this.replayService = replayService;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/endpoints/{endpointId}/events")
    public PageResponse<EventSummary> list(@PathVariable UUID endpointId,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "50") int size) {
        int capped = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageResponse.of(
                events.list(currentUser.id(), endpointId, PageRequest.of(Math.max(page, 0), capped)),
                EventSummary::from);
    }

    @GetMapping("/api/events/{id}")
    public EventDetailResponse get(@PathVariable UUID id) {
        return EventDetailResponse.from(events.detail(currentUser.id(), id));
    }

    @PostMapping("/api/events/{id}/replay")
    public Map<String, Object> replay(@PathVariable UUID id) {
        int queued = replayService.replay(currentUser.id(), id);
        return Map.of("queued", queued);
    }
}
