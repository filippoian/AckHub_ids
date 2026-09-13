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
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission non trovata"));

        TeamRegistration registration = teamRegistrationRepository.findById(submission.getRegistrationId())
                .orElseThrow(() -> new IllegalStateException("Team registration not found"));
        long hackathonId = registration.getHackathonId();

        // Solo i judge assegnati possono valutare.
        boolean isJudge = staffAssignmentRepository.findByHackathonIdAndRole(hackathonId, StaffRole.JUDGE)
                .stream()
                .anyMatch(assignment -> assignment.getStaffId() == currentStaffId);
        if (!isJudge) {
            throw new IllegalArgumentException("Not authorized: only judge can evaluate");
        }

        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new IllegalArgumentException("Hackathon not found"));
        // La valutazione e' ammessa solo nella fase REVIEW.
        if (!hackathon.canEvaluate() || hackathon.getWinnerTeamId() != null) {
            throw new IllegalStateException("Hackathon not in REVIEW");
        }
        // Da qui in poi il team non puo' piu' essere valutato se espulso.
        if (registration.isExpelled()) {
            throw new IllegalStateException("Cannot evaluate expelled team submission");
        }
    }

    // Verifica rapida usata quando il client ha gia' hackathon e judge.
    public void assertHackathonInReviewForJudge(long currentStaffId, long hackathonId) {
        boolean isJudge = staffAssignmentRepository.findByHackathonIdAndRole(hackathonId, StaffRole.JUDGE)
                .stream()
                .anyMatch(assignment -> assignment.getStaffId() == currentStaffId);
        if (!isJudge) {
            throw new IllegalArgumentException("Non sei giudice assegnato a questo hackathon.");
        }

        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new IllegalArgumentException("Hackathon not found"));
        if (!hackathon.canEvaluate() || hackathon.getWinnerTeamId() != null) {
            throw new IllegalStateException("Hackathon not in REVIEW");
        }
    }

    // Salva il voto: aggiorna quello esistente oppure ne crea uno nuovo.
    @Transactional
    public Evaluation evaluateSubmission(long currentStaffId, long submissionId, int score, String comment) {
        if (score < 0 || score > 30) {
            throw new IllegalArgumentException("Score must be between 0 and 30");
        }

        if (comment == null || comment.isBlank()) throw new IllegalArgumentException("Commento obbligatorio");
        assertEvaluatable(currentStaffId, submissionId);

        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission non trovata"));

        TeamRegistration registration = teamRegistrationRepository.findById(submission.getRegistrationId()).orElseThrow();
        List<ViolationReport> warnings = teamWarnings(registration.getHackathonId(), registration.getTeamId());
        if (warnings.stream().anyMatch(w -> !w.isPenaltyEvaluated())) {
            throw new IllegalStateException("Valutare tutte le ammonizioni del team prima della sottomissione");
        }
        int total = totalPenalty(warnings);
        LocalDateTime now = LocalDateTime.now();
        // Aggiorno la valutazione esistente, altrimenti ne creo una nuova.
        Evaluation evaluation = evaluationRepository.findBySubmissionId(submissionId)
                .map(existing -> {
                    existing.setScore(score);
                    existing.setComment(comment);
                    existing.setEvaluatedAt(now);
                    return existing;
                })
                .orElseGet(() -> new Evaluation(
                        0L,
                        submissionId,
                        score,
                        comment,
                        now
                ));

        evaluation.setPenalty(total);
        return evaluationRepository.save(evaluation);
    }

    @Transactional
    public ViolationReport evaluateWarning(long currentStaffId, long reportId, int penalty) {
        if (penalty < 0 || penalty > 30) throw new IllegalArgumentException("Penalità fuori intervallo 0–30");
        ViolationReport report = violationReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Segnalazione non trovata"));
        assertHackathonInReviewForJudge(currentStaffId, report.getHackathonId());
        if (!"TEAM_WARNED".equals(report.getDecision())) throw new IllegalStateException("Segnalazione non ammonita");
        TeamRegistration registration = teamRegistrationRepository
                .findByTeamIdAndHackathonId(report.getTeamId(), report.getHackathonId()).orElseThrow();
        if (registration.isExpelled()) throw new IllegalStateException("Team espulso");
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
