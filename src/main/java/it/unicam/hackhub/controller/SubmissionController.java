package it.unicam.hackhub.controller;

import it.unicam.hackhub.model.Hackathon;
import it.unicam.hackhub.model.Submission;
import it.unicam.hackhub.model.TeamRegistration;
import it.unicam.hackhub.model.User;
import it.unicam.hackhub.repository.HackathonRepository;
import it.unicam.hackhub.repository.SubmissionRepository;
import it.unicam.hackhub.repository.TeamRegistrationRepository;
import it.unicam.hackhub.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class SubmissionController {
    private final TeamRegistrationRepository teamRegistrationRepository;
    private final HackathonRepository hackathonRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;

    public SubmissionController(TeamRegistrationRepository teamRegistrationRepository,
                                HackathonRepository hackathonRepository,
                                SubmissionRepository submissionRepository,
                                UserRepository userRepository) {
        this.teamRegistrationRepository = teamRegistrationRepository;
        this.hackathonRepository = hackathonRepository;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
    }

    // Crea o aggiorna la submission del team dell'utente corrente.
    public Submission submitOrUpdate(long currentUserId, long teamId, String content) {
        if (teamId <= 0) {
            throw new IllegalArgumentException("Team not found");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Submission content is required");
        }

        User user = userRepository.findById(currentUserId).filter(User::isActive)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        // La submission si puo' toccare solo dal proprio team.
        if (user.getTeamId() == null || user.getTeamId() != teamId) {
            throw new IllegalArgumentException("User is not a member of this team");
        }

        TeamRegistration registration = teamRegistrationRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team is not registered to any hackathon"));
        // Un team espulso non puo' inviare o modificare submission.
        if (registration.isExpelled()) {
            throw new IllegalStateException("Team expelled");
        }

        Hackathon hackathon = hackathonRepository.findById(registration.getHackathonId())
                .orElseThrow(() -> new IllegalArgumentException("Hackathon not found"));
        // Per inviare deve essere nella fase giusta.
        if (!hackathon.canSubmit()) {
            throw new IllegalStateException("Hackathon is not accepting submissions");
        }
        LocalDateTime now = LocalDateTime.now();
        if (hackathon.getSubmissionDeadline() != null && now.isAfter(hackathon.getSubmissionDeadline())) {
            throw new IllegalStateException("Scadenza invio sottomissione superata.");
        }

        // Se esiste gia', la aggiorno; altrimenti creo la prima submission.
        Submission submission = submissionRepository.findByRegistrationId(registration.getRegistrationId())
                .map(existing -> {
                    existing.setContent(content);
                    existing.setLastUpdatedAt(now);
                    return existing;
                })
                .orElseGet(() -> new Submission(
                        0L,
                        registration.getRegistrationId(),
                        content,
                        now,
                        now
                ));

        return submissionRepository.save(submission);
    }

    // Restituisce la submission del team, se presente.
    public Optional<Submission> viewMySubmission(long teamId) {
        if (teamId <= 0) {
            return Optional.empty();
        }

        Optional<TeamRegistration> registrationOpt = teamRegistrationRepository.findByTeamId(teamId);
        if (registrationOpt.isEmpty()) {
            return Optional.empty();
        }
        return submissionRepository.findByRegistrationId(registrationOpt.get().getRegistrationId());
    }
}
