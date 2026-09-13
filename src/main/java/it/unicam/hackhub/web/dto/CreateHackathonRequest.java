package it.unicam.hackhub.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CreateHackathonRequest(
        String name,
        String regulation,
        LocalDateTime registrationDeadline,
        LocalDateTime startDate,
        LocalDateTime endDate,
        LocalDateTime submissionDeadline,
        String location,
        BigDecimal prizeAmount,
        int maxTeamSize,
        long judgeId,
        List<Long> mentorIds
) {
}
