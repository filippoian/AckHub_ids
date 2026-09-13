package it.unicam.hackhub.web;

import it.unicam.hackhub.controller.HackathonController;
import it.unicam.hackhub.model.Hackathon;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/hackathons")
public class HackathonWebController {
    private final HackathonController hackathonController;

    public HackathonWebController(HackathonController hackathonController) {
        this.hackathonController = hackathonController;
    }

    // Restituisce la lista hackathon per il client web.
    @GetMapping
    public List<HackathonDto> listHackathons() {
        return hackathonController.listHackathons().stream()
                .map(HackathonDto::from)
                .toList();
    }

    // Restituisce il dettaglio di un hackathon specifico.
    @GetMapping("/{id}")
    public HackathonDto getHackathon(@PathVariable long id) {
        return hackathonController.getHackathonDetails(id)
                .map(HackathonDto::from)
                .orElseThrow(() -> new IllegalArgumentException("Hackathon not found"));
    }

    private record HackathonDto(
            long id,
            String name,
            String regulation,
            String status,
            String location,
            LocalDateTime registrationDeadline,
            LocalDateTime startDate,
            LocalDateTime endDate,
            LocalDateTime submissionDeadline,
            BigDecimal prizeAmount,
            int maxTeamSize,
            Long winnerTeamId
    ) {
        private static HackathonDto from(Hackathon hackathon) {
            return new HackathonDto(
                    hackathon.getHackathonId(),
                    hackathon.getHackathonName(),
                    hackathon.getRegulation(),
                    hackathon.getStatus().name(),
                    hackathon.getLocation(),
                    hackathon.getRegistrationDeadline(),
                    hackathon.getStartDate(),
                    hackathon.getEndDate(),
                    hackathon.getSubmissionDeadline(),
                    hackathon.getPrizeAmount(),
                    hackathon.getMaxTeamSize(),
                    hackathon.getWinnerTeamId()
            );
        }
    }
}
