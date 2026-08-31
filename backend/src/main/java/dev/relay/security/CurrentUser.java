package dev.relay.security;

import org.springframework.web.context.annotation.RequestScope;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequestScope
public class CurrentUser {

    private UUID userId;

    public UUID id() {
        if (userId == null) {
            throw new IllegalStateException("No authenticated user on this request");
        }
        return userId;
    }

    public void set(UUID userId) {
        this.userId = userId;
    }
}
