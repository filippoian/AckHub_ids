package it.unicam.hackhub.external;

import java.time.LocalDateTime;

public interface CalendarSystem {
    String createMeetingLink(LocalDateTime start, LocalDateTime end);

    default String getLastErrorMessage() {
        return null;
    }
}
