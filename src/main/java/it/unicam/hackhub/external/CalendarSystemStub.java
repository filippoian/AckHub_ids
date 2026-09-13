package it.unicam.hackhub.external;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class CalendarSystemStub implements CalendarSystem {
    private String lastErrorMessage;
    private final AtomicLong seq = new AtomicLong(1);
    private final List<Slot> slots = new ArrayList<>();

    private static final class Slot {
        final LocalDateTime start;
        final LocalDateTime end;
        final String link;

        Slot(LocalDateTime start, LocalDateTime end, String link) {
            this.start = start;
            this.end = end;
            this.link = link;
        }
    }

    @Override
    public String createMeetingLink(LocalDateTime start, LocalDateTime end) {
        lastErrorMessage = null;

        if (start == null || end == null || !start.isBefore(end)) {
            lastErrorMessage = "Intervallo non valido";
            return null;
        }

        for (Slot slot : slots) {
            if (start.isBefore(slot.end) && end.isAfter(slot.start)) {
                lastErrorMessage = "Slot gia occupato";
                return null;
            }
        }

        long n = seq.getAndIncrement();
        String id = String.format("MEET-%06d", n);
        String link = "https://calendar.stub/meeting/" + id;
        slots.add(new Slot(start, end, link));
        return link;
    }

    @Override
    public String getLastErrorMessage() {
        return lastErrorMessage;
    }
}
