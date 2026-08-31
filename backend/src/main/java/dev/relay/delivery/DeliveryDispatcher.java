package dev.relay.delivery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

@Component
public class DeliveryDispatcher {

    private static final Logger log = LoggerFactory.getLogger(DeliveryDispatcher.class);

    private final DeliveryQueue queue;
    private final DeliveryProcessor processor;
    private final ExecutorService deliveryExecutor;

    public DeliveryDispatcher(DeliveryQueue queue,
                              DeliveryProcessor processor,
                              ExecutorService deliveryExecutor) {
        this.queue = queue;
        this.processor = processor;
        this.deliveryExecutor = deliveryExecutor;
    }

    public int dispatch() {
        List<UUID> claimed = queue.claimDue();
        for (UUID deliveryId : claimed) {
            deliveryExecutor.execute(() -> safeProcess(deliveryId));
        }
        return claimed.size();
    }

    private void safeProcess(UUID deliveryId) {
        try {
            processor.process(deliveryId);
        } catch (Exception ex) {
            log.warn("Delivery {} failed to process; will retry after lease expiry", deliveryId, ex);
        }
    }
}
