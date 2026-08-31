package dev.relay.security;

import dev.relay.user.User;
import dev.relay.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Authenticates management API calls by a shared API key sent in {@code X-Api-Key}.
 * Only guards {@code /api/**}; ingest and actuator endpoints are left open.
 */
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-Api-Key";

    private final UserRepository users;
    private final TokenGenerator tokenGenerator;
    private final CurrentUser currentUser;

    public ApiKeyAuthFilter(UserRepository users, TokenGenerator tokenGenerator, CurrentUser currentUser) {
        this.users = users;
        this.tokenGenerator = tokenGenerator;
        this.currentUser = currentUser;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey == null || apiKey.isBlank()) {
            unauthorized(response);
            return;
        }

        Optional<User> user = users.findByApiKeyHash(tokenGenerator.sha256(apiKey));
        if (user.isEmpty()) {
            unauthorized(response);
            return;
        }

        currentUser.set(user.get().getId());
        chain.doFilter(request, response);
    }

    private void unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "ApiKey");
        response.getWriter().write("{\"error\":\"unauthorized\"}");
    }
}
