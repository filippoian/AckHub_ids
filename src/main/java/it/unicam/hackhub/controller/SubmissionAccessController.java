package it.unicam.hackhub.controller;

import it.unicam.hackhub.model.Hackathon;
import it.unicam.hackhub.model.StaffAssignment;
import it.unicam.hackhub.model.Submission;
import it.unicam.hackhub.model.Team;
import it.unicam.hackhub.model.TeamRegistration;
import it.unicam.hackhub.model.ViolationReport;
import it.unicam.hackhub.model.enums.StaffRole;
import it.unicam.hackhub.repository.EvaluationRepository;
import it.unicam.hackhub.repository.HackathonRepository;
import it.unicam.hackhub.repository.StaffAssignmentRepository;
import it.unicam.hackhub.repository.SubmissionRepository;
import it.unicam.hackhub.repository.TeamRegistrationRepository;
import it.unicam.hackhub.repository.TeamRepository;
import it.unicam.hackhub.repository.ViolationReportRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class SubmissionAccessController {
    private final HackathonRepository hackathonRepository;
    private final StaffAssignmentRepository staffAssignmentRepository;
    private final TeamRegistrationRepository teamRegistrationRepository;
    private final SubmissionRepository submissionRepository;
    private final TeamRepository teamRepository;
    private final EvaluationRepository evaluationRepository;
    private final ViolationReportRepository violationReportRepository;

    public SubmissionAccessController(HackathonRepository hackathonRepository,
                                      StaffAssignmentRepository staffAssignmentRepository,
                                      TeamRegistrationRepository teamRegistrationRepository,
                                      SubmissionRepository submissionRepository,
                                      TeamRepository teamRepository,
                                      EvaluationRepository evaluationRepository,
                                      ViolationReportRepository violationReportRepository) {
        this.hackathonRepository = hackathonRepository;
        this.staffAssignmentRepository = staffAssignmentRepository;
        this.teamRegistrationRepository = teamRegistrationRepository;
        this.submissionRepository = submissionRepository;
        this.teamRepository = teamRepository;
        this.evaluationRepository = evaluationRepository;
        this.violationReportRepository = violationReportRepository;
    }

    // Elenca gli hackathon assegnati allo staff con i ruoli trovati.
    public List<AssignedHackathonView> listAssignedHackathons(long currentStaffId) {
        Map<Long, List<StaffAssignment>> assignmentsByHackathon = staffAssignmentRepository.findByStaffId(currentStaffId).stream()
                .collect(Collectors.groupingBy(StaffAssignment::getHackathonId));

        return assignmentsByHackathon.entrySet().stream()
                .filter(entry -> hackathonRepository.findById(entry.getKey()).isPresent())
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    long hackathonId = entry.getKey();
                    Hackathon hackathon = hackathonRepository.findById(hackathonId).orElse(null);
                    Set<StaffRole> roles = entry.getValue().stream()
                            .map(StaffAssignment::getRole)
                            .distinct()
                            .sorted(Comparator.comparing(Enum::name))
                            .collect(Collectors.toCollection(LinkedHashSet::new));
                    return new AssignedHackathonView(
                            hackathonId,
                            hackathon == null ? "N/A" : safe(hackathon.getHackathonName()),
                            hackathon == null || hackathon.getStatus() == null ? "-" : hackathon.getStatus().name(),
                            hackathon == null ? "-" : safe(hackathon.getLocation()),
                            roles
                    );
                })
                .toList();
    }

    public List<AssignedHackathonView> listEvaluatableHackathons(long currentStaffId) {
        return listAssignedHackathons(currentStaffId).stream()
                .filter(event -> event.roles().contains(StaffRole.JUDGE))
                .filter(event -> hackathonRepository.findById(event.hackathonId())
                        .filter(hackathon -> hackathon.canEvaluate() && hackathon.getWinnerTeamId() == null)
                        .isPresent())
                .toList();
    }

    private void requireAssignment(long currentStaffId, long hackathonId) {
        hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new IllegalArgumentException("Hackathon non trovato"));
        if (staffAssignmentRepository.findByHackathonId(hackathonId).stream()
                .noneMatch(assignment -> assignment.getStaffId() == currentStaffId)) {
            throw new IllegalArgumentException("Staff non assegnato a questo hackathon");
        }
    }

    // Restituisce le submission di un hackathon solo allo staff assegnato.
    public List<Submission> listSubmissionsForHackathon(long currentStaffId, long hackathonId) {
        requireAssignment(currentStaffId, hackathonId);

        List<TeamRegistration> registrations = teamRegistrationRepository.findByHackathonId(hackathonId);
        List<Submission> submissions = new ArrayList<>();
        for (TeamRegistration registration : registrations) {
            submissionRepository.findByRegistrationId(registration.getRegistrationId())
                    .ifPresent(submissions::add);
        }
        return submissions;
    }

    // Restituisce le submission in formato leggero per le liste.
    public List<SubmissionView> listSubmissionViewsForHackathon(long currentStaffId, long hackathonId) {
        requireAssignment(currentStaffId, hackathonId);
        Map<Long, List<ViolationReport>> warningsByTeam = warningsByTeam(hackathonId);
        return teamRegistrationRepository.findByHackathonId(hackathonId).stream()
                .flatMap(registration -> submissionRepository.findByRegistrationId(registration.getRegistrationId())
                        .map(submission -> view(submission, registration, warningsByTeam)).stream())
                .sorted(Comparator.comparingLong(SubmissionView::submissionId))
                .toList();
    }

    public Optional<SubmissionDetailView> getSubmissionDetailForHackathon(long currentStaffId,
                                                                           long hackathonId,
                                                                           long submissionId) {
        requireAssignment(currentStaffId, hackathonId);
        return submissionRepository.findById(submissionId).flatMap(submission ->
                teamRegistrationRepository.findById(submission.getRegistrationId())
                        .filter(registration -> registration.getHackathonId() == hackathonId)
                        .map(registration -> {
                            Map<Long, List<ViolationReport>> warnings = warningsByTeam(hackathonId);
                            SubmissionView summary = view(submission, registration, warnings);
                            return new SubmissionDetailView(summary.submissionId(), summary.registrationId(),
                                    summary.updatedAt(), summary.content(), summary.teamId(), summary.teamName(),
                                    summary.disciplinaryStatus(), summary.evaluated(),
                                    warnings.getOrDefault(summary.teamId(), List.of()).stream()
                                            .map(WarningView::from).toList());
                        }));
    }

    private Map<Long, List<ViolationReport>> warningsByTeam(long hackathonId) {
        return violationReportRepository.findByHackathonId(hackathonId).stream()
                .filter(report -> "TEAM_WARNED".equals(report.getDecision()))
                .collect(Collectors.groupingBy(ViolationReport::getTeamId));
    }

    private SubmissionView view(Submission submission, TeamRegistration registration,
                                Map<Long, List<ViolationReport>> warningsByTeam) {
        long teamId = registration.getTeamId();
        String name = teamRepository.findById(teamId).map(Team::getTeamName).orElse(null);
        String status = registration.isExpelled() ? "TEAM_EXPELLED"
                : warningsByTeam.containsKey(teamId) ? "TEAM_WARNED" : "NONE";
        return new SubmissionView(submission.getSubmissionId(), submission.getRegistrationId(),
                submission.getLastUpdatedAt(), submission.getContent(), teamId, name, status,
                evaluationRepository.findBySubmissionId(submission.getSubmissionId()).isPresent());
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }

    public record AssignedHackathonView(long hackathonId,
                                        String name,
                                        String status,
                                        String location,
                                        Set<StaffRole> roles) {
        // Mostra i ruoli in una stringa compatta per CLI/API.
        public String rolesLabel() {
            if (roles == null || roles.isEmpty()) {
                return "-";
            }
            return roles.stream()
                    .map(Enum::name)
                    .sorted()
                    .collect(Collectors.joining(","));
        }
    }

    public record SubmissionView(long submissionId,
                                 long registrationId,
                                 LocalDateTime updatedAt,
                                 String content,
                                 long teamId,
                                 String teamName,
                                 String disciplinaryStatus,
                                 boolean evaluated) {
    }

    public record SubmissionDetailView(long submissionId,
                                       long registrationId,
                                       LocalDateTime updatedAt,
                                       String content,
                                       long teamId,
                                       String teamName,
                                       String disciplinaryStatus,
                                       boolean evaluated,
                                       List<WarningView> warnings) {
    }

    public record WarningView(long reportId, String reason, Integer penalty, boolean evaluated) {
        private static WarningView from(ViolationReport report) {
            return new WarningView(report.getReportId(), report.getDescription(),
                    report.isPenaltyEvaluated() ? report.getPenalty() : null, report.isPenaltyEvaluated());
        }
    }
}
