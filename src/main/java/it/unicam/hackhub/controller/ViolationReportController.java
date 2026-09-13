package it.unicam.hackhub.controller;

import it.unicam.hackhub.model.Hackathon;
import it.unicam.hackhub.model.StaffAssignment;
import it.unicam.hackhub.model.Team;
import it.unicam.hackhub.model.TeamRegistration;
import it.unicam.hackhub.model.ViolationReport;
import it.unicam.hackhub.model.enums.HackathonStatus;
import it.unicam.hackhub.model.enums.StaffRole;
import it.unicam.hackhub.repository.HackathonRepository;
import it.unicam.hackhub.repository.StaffAssignmentRepository;
import it.unicam.hackhub.repository.TeamRepository;
import it.unicam.hackhub.repository.TeamRegistrationRepository;
import it.unicam.hackhub.repository.ViolationReportRepository;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ViolationReportController {
    private final HackathonRepository hackathonRepository;
    private final StaffAssignmentRepository staffAssignmentRepository;
    private final TeamRepository teamRepository;
    private final TeamRegistrationRepository teamRegistrationRepository;
    private final ViolationReportRepository violationReportRepository;

    public ViolationReportController(HackathonRepository hackathonRepository,
                                     StaffAssignmentRepository staffAssignmentRepository,
                                     TeamRepository teamRepository,
                                     TeamRegistrationRepository teamRegistrationRepository,
                                     ViolationReportRepository violationReportRepository) {
        this.hackathonRepository = hackathonRepository;
        this.staffAssignmentRepository = staffAssignmentRepository;
        this.teamRepository = teamRepository;
        this.teamRegistrationRepository = teamRegistrationRepository;
        this.violationReportRepository = violationReportRepository;
    }

    // Restituisce gli hackathon in cui lo staff e' mentor.
    public List<MentorHackathonOption> listMentorHackathons(long currentStaffId) {
        Set<Long> hackathonIds = new LinkedHashSet<>();
        for (StaffAssignment assignment : staffAssignmentRepository.findByStaffId(currentStaffId)) {
            if (assignment.getRole() == StaffRole.MENTOR) {
                hackathonIds.add(assignment.getHackathonId());
            }
        }

        return hackathonIds.stream()
                .filter(id -> hackathonRepository.findById(id).isPresent())
                .map(hackathonId -> {
                    Hackathon hackathon = hackathonRepository.findById(hackathonId).orElse(null);
                    return new MentorHackathonOption(
                            hackathonId,
                            hackathon == null ? "N/A" : safe(hackathon.getHackathonName())
                    );
                })
                .toList();
    }

    // Elenca i team segnalabili in un hackathon dove il mentor e' assegnato.
    public List<ReportTeamOption> listReportableTeams(long currentStaffId, long hackathonId) {
        boolean mentorAssigned = false;
        for (StaffAssignment assignment : staffAssignmentRepository.findByHackathonId(hackathonId)) {
            if (assignment.getRole() == StaffRole.MENTOR && assignment.getStaffId() == currentStaffId) {
                mentorAssigned = true;
                break;
            }
        }
        if (!mentorAssigned) {
            throw new IllegalArgumentException("Comando disponibile solo per MENTOR.");
        }

        List<TeamRegistration> registrations = teamRegistrationRepository.findByHackathonId(hackathonId);
        return registrations.stream()
                .map(registration -> {
                    Team team = teamRepository.findById(registration.getTeamId()).orElse(null);
                    return new ReportTeamOption(
                            registration.getTeamId(),
                            team == null ? "N/A" : safe(team.getTeamName()),
                            registration.isExpelled()
                    );
                })
                .toList();
    }

    // Restituisce gli hackathon in cui lo staff ha ruolo organizer.
    public List<OrganizerHackathonOption> listOrganizerHackathons(long currentStaffId) {
        return staffAssignmentRepository.findByStaffId(currentStaffId).stream()
                .filter(assignment -> assignment.getRole() == StaffRole.ORGANIZER)
                .map(StaffAssignment::getHackathonId)
                .distinct()
                .filter(id -> hackathonRepository.findById(id).isPresent())
                .map(hackathonId -> {
                    Hackathon hackathon = hackathonRepository.findById(hackathonId).orElse(null);
                    return new OrganizerHackathonOption(
                            hackathonId,
                            hackathon == null ? "N/A" : safe(hackathon.getHackathonName())
                    );
                })
                .toList();
    }

    // Elenca le segnalazioni, con filtro opzionale sulle sole pendenti.
    public List<ViolationReport> listReports(long currentStaffId, long hackathonId, boolean onlyPending) {
        hackathonRepository.findById(hackathonId).orElseThrow(() -> new IllegalArgumentException("Hackathon non trovato"));
        boolean organizerAssigned = false;
        for (StaffAssignment assignment : staffAssignmentRepository.findByHackathonId(hackathonId)) {
            if (assignment.getRole() == StaffRole.ORGANIZER && assignment.getStaffId() == currentStaffId) {
                organizerAssigned = true;
                break;
            }
        }
        if (!organizerAssigned) {
            throw new IllegalArgumentException("Not authorized: only organizer can view reports");
        }

        if (onlyPending) {
            return violationReportRepository.findPendingByHackathonId(hackathonId);
        }
        return violationReportRepository.findByHackathonId(hackathonId);
    }

    // Crea una segnalazione disciplinare per un team registrato.
    public ViolationReport createReport(long currentStaffId, long hackathonId, long teamId, String description) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new IllegalArgumentException("Hackathon non trovato"));
        // Le segnalazioni hanno senso solo mentre la gara e' in corso.
        if (hackathon.getStatus() != HackathonStatus.RUNNING) {
            throw new IllegalStateException("Segnalazioni consentite solo in stato RUNNING");
        }

        boolean mentorAssigned = false;
        for (StaffAssignment assignment : staffAssignmentRepository.findByHackathonId(hackathonId)) {
            if (assignment.getRole() == StaffRole.MENTOR && assignment.getStaffId() == currentStaffId) {
                mentorAssigned = true;
                break;
            }
        }
        if (!mentorAssigned) {
            throw new IllegalArgumentException("Not authorized: only mentor can create report");
        }

        TeamRegistration registration = teamRegistrationRepository.findByTeamIdAndHackathonId(teamId, hackathonId)
                .orElseThrow(() -> new IllegalArgumentException("Team not registered to this hackathon"));
        if (registration.isExpelled()) throw new IllegalStateException("Team espulso");

        ViolationReport report = new ViolationReport(
                0L,
                hackathonId,
                teamId,
                currentStaffId,
                description,
                LocalDateTime.now(),
                null
        );
        return violationReportRepository.save(report);
    }

    // Applica la decisione organizer: rifiuto, ammonizione o espulsione.
    @org.springframework.transaction.annotation.Transactional
    public ViolationReport manageReport(long currentStaffId, long reportId, String action) {
        ViolationReport report = violationReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        boolean organizerAssigned = false;
        for (StaffAssignment assignment : staffAssignmentRepository.findByHackathonId(report.getHackathonId())) {
            if (assignment.getRole() == StaffRole.ORGANIZER && assignment.getStaffId() == currentStaffId) {
                organizerAssigned = true;
                break;
            }
        }
        if (!organizerAssigned) {
            throw new IllegalArgumentException("Not authorized: only organizer can manage reports");
        }

        if (report.getDecision() != null) {
            throw new IllegalStateException("Report already managed");
        }

        Hackathon event = hackathonRepository.findById(report.getHackathonId()).orElseThrow();
        if (event.getWinnerTeamId() != null || (event.getStatus() != HackathonStatus.RUNNING && event.getStatus() != HackathonStatus.REVIEW)) {
            throw new IllegalStateException("Decisioni non modificabili dopo la proclamazione o chiusura");
        }
        if ("WARN".equalsIgnoreCase(action)) {
            TeamRegistration registration = teamRegistrationRepository
                    .findByTeamIdAndHackathonId(report.getTeamId(), report.getHackathonId()).orElseThrow();
            if (registration.isExpelled()) throw new IllegalStateException("Team espulso");
            report.setDecision("TEAM_WARNED");
            report.setManagedByStaffId(currentStaffId);
            return violationReportRepository.save(report);
        }
        // REJECT chiude il report senza toccare la registrazione.
        if ("REJECT".equalsIgnoreCase(action)) {
            report.setManagedByStaffId(currentStaffId);
            report.setDecision("REJECTED");
            return violationReportRepository.save(report);
        }

        if ("EXPEL".equalsIgnoreCase(action)) {
            TeamRegistration registration = teamRegistrationRepository
                    .findByTeamIdAndHackathonId(report.getTeamId(), report.getHackathonId())
                    .orElseThrow(() -> new IllegalArgumentException("Team not registered to this hackathon"));
            // L'espulsione aggiorna sia la registrazione sia il report.
            registration.setExpelled(true);
            teamRegistrationRepository.save(registration);

            report.setManagedByStaffId(currentStaffId);
            report.setDecision("TEAM_EXPELLED");
            return violationReportRepository.save(report);
        }

        throw new IllegalArgumentException("Invalid action");
    }

    public List<ViolationReport> listWarnings(long currentStaffId, long hackathonId) {
        hackathonRepository.findById(hackathonId).orElseThrow(() -> new IllegalArgumentException("Hackathon non trovato"));
        boolean assigned = staffAssignmentRepository.findByHackathonIdAndRole(hackathonId, StaffRole.JUDGE)
                .stream().anyMatch(a -> a.getStaffId() == currentStaffId);
        if (!assigned) throw new IllegalArgumentException("Solo il giudice assegnato può consultare le ammonizioni");
        return violationReportRepository.findByHackathonId(hackathonId).stream()
                .filter(r -> "TEAM_WARNED".equals(r.getDecision())).toList();
    }

    @org.springframework.transaction.annotation.Transactional
    public ViolationReport updateReport(long currentStaffId, long reportId, String description) {
        ViolationReport report = requireEditableReport(currentStaffId, reportId);
        if (description == null || description.isBlank()) throw new IllegalArgumentException("Descrizione obbligatoria");
        report.setDescription(description.trim());
        return violationReportRepository.save(report);
    }

    @org.springframework.transaction.annotation.Transactional
    public void withdrawReport(long currentStaffId, long reportId) {
        ViolationReport report = requireEditableReport(currentStaffId, reportId);
        report.setDecision("WITHDRAWN");
        violationReportRepository.save(report);
    }

    public List<ViolationReport> listExpulsions(long currentStaffId, long hackathonId) {
        hackathonRepository.findById(hackathonId).orElseThrow(() -> new IllegalArgumentException("Hackathon non trovato"));
        boolean assigned = staffAssignmentRepository.findByHackathonIdAndRole(hackathonId, StaffRole.JUDGE)
                .stream().anyMatch(a -> a.getStaffId() == currentStaffId);
        if (!assigned) throw new IllegalArgumentException("Solo il giudice assegnato può consultare le espulsioni");
        return violationReportRepository.findByHackathonId(hackathonId).stream()
                .filter(r -> "TEAM_EXPELLED".equals(r.getDecision())).toList();
    }

    private ViolationReport requireEditableReport(long currentStaffId, long reportId) {
        ViolationReport report = violationReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Segnalazione non trovata"));
        boolean assigned = staffAssignmentRepository.findByHackathonIdAndRole(report.getHackathonId(), StaffRole.MENTOR)
                .stream().anyMatch(a -> a.getStaffId() == currentStaffId);
        Hackathon event = hackathonRepository.findById(report.getHackathonId()).orElseThrow();
        if (report.getMentorStaffId() != currentStaffId || !assigned) throw new IllegalArgumentException("Solo il mentore autore può modificare la segnalazione");
        if (report.getDecision() != null || event.getStatus() != HackathonStatus.RUNNING)
            throw new IllegalStateException("Segnalazione non modificabile");
        return report;
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }

    public record MentorHackathonOption(long hackathonId, String hackathonName) {
    }

    public record ReportTeamOption(long teamId, String teamName, boolean expelled) {
    }

    public record OrganizerHackathonOption(long hackathonId, String hackathonName) {
    }
}
