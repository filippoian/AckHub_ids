package it.unicam.hackhub.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import it.unicam.hackhub.model.ViolationReport;
import it.unicam.hackhub.repository.ViolationReportRepository;

import it.unicam.hackhub.model.Evaluation;
import it.unicam.hackhub.model.Hackathon;
import it.unicam.hackhub.model.StaffAssignment;
import it.unicam.hackhub.model.Submission;
import it.unicam.hackhub.model.TeamRegistration;
import it.unicam.hackhub.model.enums.StaffRole;
import it.unicam.hackhub.repository.EvaluationRepository;
import it.unicam.hackhub.repository.HackathonRepository;
import it.unicam.hackhub.repository.StaffAssignmentRepository;
import it.unicam.hackhub.repository.SubmissionRepository;
import it.unicam.hackhub.repository.TeamRegistrationRepository;

@Service
public class EvaluationController {
    private final StaffAssignmentRepository staffAssignmentRepository;
    private final HackathonRepository hackathonRepository;
    private final EvaluationRepository evaluationRepository;
    private final SubmissionRepository submissionRepository;
    private final TeamRegistrationRepository teamRegistrationRepository;
    private final ViolationReportRepository violationReportRepository;

    public EvaluationController(StaffAssignmentRepository staffAssignmentRepository,
                                HackathonRepository hackathonRepository,
                                EvaluationRepository evaluationRepository,
                                SubmissionRepository submissionRepository,
                                TeamRegistrationRepository teamRegistrationRepository,
                                ViolationReportRepository violationReportRepository) {
        this.staffAssignmentRepository = staffAssignmentRepository;
        this.hackathonRepository = hackathonRepository;
        this.evaluationRepository = evaluationRepository;
        this.submissionRepository = submissionRepository;
        this.teamRegistrationRepository = teamRegistrationRepository;
        this.violationReportRepository = violationReportRepository;
    }

    // Recupera la valutazione della submission se lo staff e' assegnato all'hackathon.
    public Optional<EvaluationView> viewEvaluation(long currentStaffId, long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission non trovata."));
        TeamRegistration registration = teamRegistrationRepository.findById(submission.getRegistrationId())
                .orElseThrow(() -> new IllegalArgumentException("Registrazione non trovata."));

        List<StaffAssignment> assignments = staffAssignmentRepository.findByHackathonId(registration.getHackathonId());
        boolean assigned = assignments.stream().anyMatch(assignment -> assignment.getStaffId() == currentStaffId);
        if (!assigned) {
            throw new IllegalArgumentException("Non autorizzato: non sei assegnato a questo hackathon.");
        }

        return evaluationRepository.findBySubmissionId(submissionId)
                .map(evaluation -> new EvaluationView(
                        evaluation.getEvaluationId(),
                        evaluation.getSubmissionId(),
                        evaluation.getScore(),
                        evaluation.getBaseScore(),
                        evaluation.getPenalty(),
                        evaluation.getComment(),
                        evaluation.getEvaluatedAt()
                ));
    }

    // Controlla che la submission possa essere valutata dal judge corrente.
    public void assertEvaluatable(long currentStaffId, long submissionId) {
        TeamRegistration registration = findRegistration(findSubmission(submissionId));
        assertHackathonInReviewForJudge(currentStaffId, registration.getHackathonId());
        requireActiveTeam(registration);
    }

    public void assertHackathonInReviewForJudge(long currentStaffId, long hackathonId) {
        requireJudgeReview(currentStaffId, hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new IllegalArgumentException("Hackathon non trovato")));
    }

    private void requireJudgeReviewForUpdate(long currentStaffId, long hackathonId) {
        requireJudgeReview(currentStaffId, hackathonRepository.findByIdForUpdate(hackathonId)
                .orElseThrow(() -> new IllegalArgumentException("Hackathon non trovato")));
    }

    private void requireJudgeReview(long currentStaffId, Hackathon event) {
        boolean isJudge = staffAssignmentRepository.findByHackathonIdAndRole(event.getHackathonId(), StaffRole.JUDGE)
                .stream().anyMatch(assignment -> assignment.getStaffId() == currentStaffId);
        if (!isJudge) {
            throw new IllegalArgumentException("Non sei giudice assegnato a questo hackathon");
        }
        if (!event.canEvaluate() || event.getWinnerTeamId() != null) {
            throw new IllegalStateException("La valutazione è disponibile in fase REVIEW prima della proclamazione");
        }
    }

    private Submission findSubmission(long submissionId) {
        return submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Sottomissione non trovata"));
    }

    private TeamRegistration findRegistration(Submission submission) {
        return teamRegistrationRepository.findById(submission.getRegistrationId())
                .orElseThrow(() -> new IllegalStateException("Registrazione non trovata"));
    }

    private void requireActiveTeam(TeamRegistration registration) {
        if (registration.isExpelled()) {
            throw new IllegalStateException("Impossibile valutare un team espulso");
        }
    }

    @Transactional
    public Evaluation evaluateSubmission(long currentStaffId, long submissionId, int score, String comment) {
        return saveEvaluation(currentStaffId, submissionId, score, comment, false);
    }

    @Transactional
    public Evaluation updateEvaluation(long currentStaffId, long submissionId, Integer score, String comment) {
        return saveEvaluation(currentStaffId, submissionId, score, comment, true);
    }

    private Evaluation saveEvaluation(long currentStaffId, long submissionId, Integer score, String comment, boolean update) {
        if ((!update && score == null) || (score != null && (score < 0 || score > 30))) {
            throw new IllegalArgumentException("Voto base obbligatorio e compreso fra 0 e 30");
        }
        if ((!update && comment == null) || (comment != null && comment.isBlank())) {
            throw new IllegalArgumentException("Giudizio obbligatorio");
        }
        if (update && score == null && comment == null) {
            throw new IllegalArgumentException("Specificare il voto base o il giudizio da modificare");
        }
        Submission submission = findSubmission(submissionId);
        long hackathonId = findRegistration(submission).getHackathonId();
        requireJudgeReviewForUpdate(currentStaffId, hackathonId);
        TeamRegistration registration = teamRegistrationRepository.findByIdForUpdate(submission.getRegistrationId())
                .orElseThrow(() -> new IllegalStateException("Registrazione non trovata"));
        requireActiveTeam(registration);

        Optional<Evaluation> existing = evaluationRepository.findBySubmissionId(submissionId);
        if (update && existing.isEmpty()) {
            throw new IllegalStateException("Nessuna valutazione da modificare");
        }
        if (!update && existing.isPresent()) {
            throw new IllegalStateException("Sottomissione già valutata. Apri Modifica valutazione.");
        }

        List<ViolationReport> warnings = teamWarnings(registration.getHackathonId(), registration.getTeamId());
        List<Long> pendingReportIds = warnings.stream()
                .filter(w -> !w.isPenaltyEvaluated())
                .map(ViolationReport::getReportId)
                .toList();
        if (!pendingReportIds.isEmpty()) {
            throw new IllegalStateException(
                    "Valutare tutte le ammonizioni del team prima della sottomissione: reportId non valutati "
                            + pendingReportIds);
        }
        int total = totalPenalty(warnings);
        LocalDateTime now = LocalDateTime.now();
        Evaluation evaluation = existing.orElseGet(() -> new Evaluation(0L, submissionId, score, comment.trim(), now));
        if (update) {
            if (score != null) {
                evaluation.setScore(score);
            }
            if (comment != null) {
                evaluation.setComment(comment.trim());
            }
            evaluation.setEvaluatedAt(now);
        }
        evaluation.setPenalty(total);
        return evaluationRepository.save(evaluation);
    }

    @Transactional
    public ViolationReport evaluateWarning(long currentStaffId, long reportId, int penalty) {
        if (penalty < 0 || penalty > 30) throw new IllegalArgumentException("Penalità fuori intervallo 0–30");
        ViolationReport report = violationReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Segnalazione non trovata"));
        requireJudgeReviewForUpdate(currentStaffId, report.getHackathonId());
        if (!"TEAM_WARNED".equals(report.getDecision())) throw new IllegalStateException("Segnalazione non ammonita");
        TeamRegistration registration = teamRegistrationRepository
                .findByTeamIdAndHackathonId(report.getTeamId(), report.getHackathonId())
                .flatMap(value -> teamRegistrationRepository.findByIdForUpdate(value.getRegistrationId()))
                .orElseThrow(() -> new IllegalStateException("Registrazione non trovata"));
        requireActiveTeam(registration);
        // Sostituisce la penalità precedente: non la somma una seconda volta.
        int total = Math.addExact(penalty, totalPenalty(teamWarnings(report.getHackathonId(), report.getTeamId())
                .stream().filter(w -> w.getReportId() != reportId).toList()));
        report.setPenalty(penalty);
        report.setPenaltyEvaluated(true);
        report.setPenaltyJudgeStaffId(currentStaffId);
        violationReportRepository.save(report);
        submissionRepository.findByRegistrationId(registration.getRegistrationId())
                .flatMap(submission -> evaluationRepository.findBySubmissionId(submission.getSubmissionId()))
                .ifPresent(evaluation -> {
                    evaluation.setPenalty(total);
                    evaluationRepository.save(evaluation);
                });
        return report;
    }

    private List<ViolationReport> teamWarnings(long hackathonId, long teamId) {
        return violationReportRepository.findByHackathonId(hackathonId).stream()
                .filter(r -> r.getTeamId() == teamId && "TEAM_WARNED".equals(r.getDecision())).toList();
    }

    private int totalPenalty(List<ViolationReport> warnings) {
        return warnings.stream().filter(ViolationReport::isPenaltyEvaluated)
                .map(ViolationReport::getPenalty).reduce(0, Math::addExact);
    }

    // Recupera la valutazione legata alla submission, se presente.
    public Optional<Evaluation> findBySubmissionId(long submissionId) {
        return evaluationRepository.findBySubmissionId(submissionId);
    }

    public record EvaluationView(long evaluationId,
                                 long submissionId,
                                 int score,
                                 int baseScore,
                                 int penalty,
                                 String comment,
                                 LocalDateTime evaluatedAt) {
    }
}
