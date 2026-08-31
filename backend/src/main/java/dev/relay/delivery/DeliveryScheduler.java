package dev.relay.delivery;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DeliveryScheduler {

    private final DeliveryDispatcher dispatcher;

    public DeliveryScheduler(DeliveryDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Scheduled(fixedDelayString = "${relay.delivery.poll-interval:1000}")
    public void poll() {
        dispatcher.dispatch();
    }
}
