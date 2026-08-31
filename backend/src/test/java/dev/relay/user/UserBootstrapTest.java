package dev.relay.user;

import dev.relay.security.TokenGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserBootstrapTest {

    private final TokenGenerator tokens = new TokenGenerator();
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-31T20:00:00Z"), ZoneOffset.UTC);

    @Test
    void rotatesConfiguredKeyForExistingBootstrapUser() throws Exception {
        UserRepository users = mock(UserRepository.class);
        User existing = new User("admin@relay.local", tokens.sha256("old-key"), clock.instant());
        when(users.findByEmail("admin@relay.local")).thenReturn(Optional.of(existing));

        new UserBootstrap(users, tokens, clock, "new-key")
                .run(new DefaultApplicationArguments(new String[0]));

        assertThat(existing.getApiKeyHash()).isEqualTo(tokens.sha256("new-key"));
        assertThat(existing.getApiKeyHash()).isNotEqualTo(tokens.sha256("old-key"));
        verify(users).save(existing);
    }

    @Test
    void leavesExistingKeyUnchangedWhenNoKeyIsConfigured() throws Exception {
        UserRepository users = mock(UserRepository.class);
        User existing = new User("admin@relay.local", tokens.sha256("generated-key"), clock.instant());
        when(users.findByEmail("admin@relay.local")).thenReturn(Optional.of(existing));

        new UserBootstrap(users, tokens, clock, "")
                .run(new DefaultApplicationArguments(new String[0]));

        assertThat(existing.getApiKeyHash()).isEqualTo(tokens.sha256("generated-key"));
    }
}
