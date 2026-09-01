package dev.relay.event;

import java.time.LocalDate;

public interface DailyEventCount {

    LocalDate getDay();

    long getTotal();
}
