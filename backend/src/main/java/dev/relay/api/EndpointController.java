package dev.relay.api;

import dev.relay.api.dto.EndpointResponse;
import dev.relay.endpoint.EndpointService;
import dev.relay.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/endpoints")
public class EndpointController {

    private final EndpointService endpoints;
    private final CurrentUser currentUser;

    public EndpointController(EndpointService endpoints, CurrentUser currentUser) {
        this.endpoints = endpoints;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<EndpointResponse> list() {
        return endpoints.list(currentUser.id()).stream().map(EndpointResponse::from).toList();
    }

    @GetMapping("/{id}")
    public EndpointResponse get(@PathVariable UUID id) {
        return EndpointResponse.from(endpoints.require(currentUser.id(), id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EndpointResponse create(@Valid @RequestBody CreateEndpointRequest request) {
        return EndpointResponse.from(endpoints.create(currentUser.id(), request.name()));
    }

    @PatchMapping("/{id}")
    public EndpointResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateEndpointRequest request) {
        return EndpointResponse.from(endpoints.setActive(currentUser.id(), id, request.active()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        endpoints.delete(currentUser.id(), id);
    }

    public record CreateEndpointRequest(@NotBlank @Size(max = 140) String name) {
    }

    public record UpdateEndpointRequest(boolean active) {
    }
}
