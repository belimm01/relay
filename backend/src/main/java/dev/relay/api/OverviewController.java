package dev.relay.api;

import dev.relay.api.dto.OverviewResponse;
import dev.relay.overview.OverviewService;
import dev.relay.security.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/overview")
public class OverviewController {

    private final OverviewService overview;
    private final CurrentUser currentUser;

    public OverviewController(OverviewService overview, CurrentUser currentUser) {
        this.overview = overview;
        this.currentUser = currentUser;
    }

    @GetMapping
    public OverviewResponse get() {
        return overview.overview(currentUser.id());
    }
}
