package it.unicam.hackhub.controller;

import it.unicam.hackhub.model.Hackathon;
import it.unicam.hackhub.model.Team;
import it.unicam.hackhub.model.TeamRegistration;
import it.unicam.hackhub.model.User;
import it.unicam.hackhub.repository.HackathonRepository;
import it.unicam.hackhub.repository.TeamRegistrationRepository;
import it.unicam.hackhub.repository.TeamRepository;
import it.unicam.hackhub.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class TeamRegistrationController {
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final HackathonRepository hackathonRepository;
    private final TeamRegistrationRepository teamRegistrationRepository;

    public TeamRegistrationController(TeamRepository teamRepository,
                                      UserRepository userRepository,
                                      HackathonRepository hackathonRepository,
                                      TeamRegistrationRepository teamRegistrationRepository) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.hackathonRepository = hackathonRepository;
        this.teamRegistrationRepository = teamRegistrationRepository;
    }

    // Mostra gli hackathon a cui il team dell'utente puo' ancora iscriversi.
    public List<HackathonRegistrationOption> listRegisterableHackathons(long currentUserId) {
        User currentUser = userRepository.findById(currentUserId).filter(User::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));
        if (currentUser.getTeamId() == null) {
            throw new IllegalArgumentException("Non hai un team. Crea un team prima di registrarti.");
        }
        if (teamRegistrationRepository.findByTeamId(currentUser.getTeamId()).isPresent()) {
            throw new IllegalArgumentException("Il tuo team e gia registrato a un hackathon.");
        }

        return hackathonRepository.findAll().stream()
                .filter(Hackathon::canRegister)
                .filter(h -> h.getRegistrationDeadline() != null && !LocalDateTime.now().isAfter(h.getRegistrationDeadline()))
                .map(h -> new HackathonRegistrationOption(
                        h.getHackathonId(),
                        h.getHackathonName(),
                        String.valueOf(h.getStatus()),
                        h.getLocation(),
                        h.getRegistrationDeadline()
                ))
                .toList();
    }

    // Registra il team all'hackathon dopo i controlli principali.
    @org.springframework.transaction.annotation.Transactional
    public TeamRegistration registerTeamToHackathon(long currentUserId, long hackathonId) {
        LocalDateTime now = LocalDateTime.now();

        User currentUser = userRepository.findById(currentUserId).filter(User::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Current user not found"));
        if (currentUser.getTeamId() == null) {
            throw new IllegalArgumentException("Current user is not in a team");
        }

        long teamId = currentUser.getTeamId();
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));

        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new IllegalArgumentException("Hackathon not found"));
        // Prima controllo stato e deadline, poi verifico che il team sia registrabile.
        if (!hackathon.canRegister()) {
            throw new IllegalArgumentException("Hackathon is not open for registration");
        }
        if (hackathon.getRegistrationDeadline() != null && now.isAfter(hackathon.getRegistrationDeadline())) {
            throw new IllegalStateException("Scadenza iscrizione superata.");
        }

        // Un team puo' risultare registrato a un solo hackathon.
        if (teamRegistrationRepository.findByTeamId(team.getTeamId()).isPresent()) {
            throw new IllegalArgumentException("Team is already registered to a hackathon");
        }

        int currentMembers = userRepository.findByTeamId(teamId).size();
        // Se supera il limite, blocchiamo la registrazione.
        if (currentMembers > hackathon.getMaxTeamSize()) {
            throw new IllegalStateException("Team troppo numeroso: supera la dimensione massima dell'hackathon.");
        }

        TeamRegistration registration = new TeamRegistration(
                0L,
                team.getTeamId(),
                hackathon.getHackathonId(),
                now,
                false
        );
        return teamRegistrationRepository.save(registration);
    }

    // Restituisce la registrazione del team dell'utente, se esiste.
    public Optional<TeamRegistration> getMyRegistration(long currentUserId) {
        User currentUser = userRepository.findById(currentUserId).filter(User::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Current user not found"));
        if (currentUser.getTeamId() == null) {
            return Optional.empty();
        }
        return teamRegistrationRepository.findByTeamId(currentUser.getTeamId());
    }

    public record HackathonRegistrationOption(long hackathonId,
                                              String name,
                                              String status,
                                              String location,
                                              LocalDateTime registrationDeadline) {
    }
}
