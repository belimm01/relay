package dev.relay.user;

import dev.relay.security.TokenGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
public class UserBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UserBootstrap.class);
    private static final String DEFAULT_EMAIL = "admin@relay.local";

    private final UserRepository users;
    private final TokenGenerator tokenGenerator;
    private final Clock clock;
    private final String configuredKey;

    public UserBootstrap(UserRepository users,
                         TokenGenerator tokenGenerator,
                         Clock clock,
                         @Value("${relay.bootstrap.api-key:}") String configuredKey) {
        this.users = users;
        this.tokenGenerator = tokenGenerator;
        this.clock = clock;
        this.configuredKey = configuredKey;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        boolean generated = configuredKey == null || configuredKey.isBlank();
        String apiKey = generated ? tokenGenerator.newToken() : configuredKey;
        users.save(new User(DEFAULT_EMAIL, tokenGenerator.sha256(apiKey), clock.instant()));

        if (generated) {
            log.info("Bootstrapped user {} — API key (shown once): {}", DEFAULT_EMAIL, apiKey);
        } else {
            log.info("Bootstrapped user {} with configured API key", DEFAULT_EMAIL);
        }
    }
}
