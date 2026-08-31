package dev.relay.api;

import dev.relay.api.dto.DestinationResponse;
import dev.relay.destination.DestinationService;
import dev.relay.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class DestinationController {

    private final DestinationService destinations;
    private final CurrentUser currentUser;

    public DestinationController(DestinationService destinations, CurrentUser currentUser) {
        this.destinations = destinations;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/endpoints/{endpointId}/destinations")
    public List<DestinationResponse> list(@PathVariable UUID endpointId) {
        return destinations.list(currentUser.id(), endpointId).stream()
                .map(DestinationResponse::from)
                .toList();
    }

    @PostMapping("/api/endpoints/{endpointId}/destinations")
    @ResponseStatus(HttpStatus.CREATED)
    public DestinationResponse create(@PathVariable UUID endpointId,
                                      @Valid @RequestBody CreateDestinationRequest request) {
        return DestinationResponse.from(destinations.create(
                currentUser.id(), endpointId, request.name(), request.targetUrl(), request.filterExpr()));
    }

    @PatchMapping("/api/destinations/{id}")
    public DestinationResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateDestinationRequest request) {
        return DestinationResponse.from(destinations.update(
                currentUser.id(), id, request.name(), request.targetUrl(), request.filterExpr(), request.active()));
    }

    @DeleteMapping("/api/destinations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        destinations.delete(currentUser.id(), id);
    }

    public record CreateDestinationRequest(
            @Size(max = 140) String name,
            @NotBlank @URL @Size(max = 2048) String targetUrl,
            @Size(max = 2048) String filterExpr) {
    }

    public record UpdateDestinationRequest(
            @Size(max = 140) String name,
            @URL @Size(max = 2048) String targetUrl,
            @Size(max = 2048) String filterExpr,
            Boolean active) {
    }
}
