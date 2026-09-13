package it.unicam.hackhub.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UpdateHackathonRequest(
        String name,
        String regulation,
        LocalDateTime registrationDeadline,
        LocalDateTime startDate,
        LocalDateTime endDate,
        LocalDateTime submissionDeadline,
        String location,
        BigDecimal prizeAmount,
        int maxTeamSize
) {
}
