package it.unicam.hackhub.web;

import it.unicam.hackhub.controller.EvaluationController;
import it.unicam.hackhub.controller.HackathonController;
import it.unicam.hackhub.controller.MentorManagementController;
import it.unicam.hackhub.controller.SubmissionAccessController;
import it.unicam.hackhub.controller.SupportController;
import it.unicam.hackhub.controller.ViolationReportController;
import it.unicam.hackhub.external.PaymentSystem;
import it.unicam.hackhub.model.CallProposal;
import it.unicam.hackhub.model.Evaluation;
import it.unicam.hackhub.model.Hackathon;
import it.unicam.hackhub.model.StaffMember;
import it.unicam.hackhub.model.ViolationReport;
import it.unicam.hackhub.repository.StaffMemberRepository;
import it.unicam.hackhub.web.dto.CreateHackathonRequest;
import it.unicam.hackhub.web.session.InMemorySessionStore;
import it.unicam.hackhub.web.session.SessionAuth;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/staff")
public class StaffWebController {
    private static final String SESSION_TOKEN_HEADER = "X-Session-Token";

    private final SubmissionAccessController submissionAccessController;
    private final StaffMemberRepository staffMemberRepository;
    private final EvaluationController evaluationController;
    private final HackathonController hackathonController;
    private final MentorManagementController mentorManagementController;
    private final PaymentSystem paymentSystem;
    private final SupportController supportController;
    private final ViolationReportController violationReportController;
    private final InMemorySessionStore sessionStore;

    public StaffWebController(SubmissionAccessController submissionAccessController,
                              StaffMemberRepository staffMemberRepository,
                              EvaluationController evaluationController,
                              HackathonController hackathonController,
                              MentorManagementController mentorManagementController,
                              PaymentSystem paymentSystem,
                              SupportController supportController,
                              ViolationReportController violationReportController,
                              InMemorySessionStore sessionStore) {
        this.submissionAccessController = submissionAccessController;
        this.staffMemberRepository = staffMemberRepository;
        this.evaluationController = evaluationController;
        this.hackathonController = hackathonController;
        this.mentorManagementController = mentorManagementController;
        this.paymentSystem = paymentSystem;
        this.supportController = supportController;
        this.violationReportController = violationReportController;
        this.sessionStore = sessionStore;
    }

    // Elenca gli hackathon assegnati allo staff loggato.
    @GetMapping("/me/hackathons")
    public List<SubmissionAccessController.AssignedHackathonView> listAssignedHackathons(
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        long staffId = SessionAuth.requireStaffId(sessionStore, token);
        return submissionAccessController.listAssignedHackathons(staffId);
    }

    // Restituisce la lista anagrafica dello staff.
    @GetMapping("/members")
    public List<StaffMemberDto> listStaffMembers(
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        SessionAuth.requireStaffId(sessionStore, token);
        return staffMemberRepository.findAll().stream()
                .sorted(Comparator.comparingLong(staffMember -> staffMember.getStaffId()))
                .map(StaffMemberDto::from)
                .toList();
    }

    // Elenca le submission di un hackathon se lo staff e' autorizzato.
    @GetMapping("/hackathons/{hackathonId}/submissions")
    public List<SubmissionAccessController.SubmissionView> listSubmissions(
            @PathVariable long hackathonId,
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        long staffId = SessionAuth.requireStaffId(sessionStore, token);
        return submissionAccessController.listSubmissionViewsForHackathon(staffId, hackathonId);
    }

    // Restituisce il dettaglio di una submission nello stesso hackathon.
    @GetMapping("/hackathons/{hackathonId}/submissions/{submissionId}")
    public SubmissionAccessController.SubmissionDetailView getSubmissionDetail(
            @PathVariable long hackathonId,
            @PathVariable long submissionId,
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        long staffId = SessionAuth.requireStaffId(sessionStore, token);
        return submissionAccessController.getSubmissionDetailForHackathon(staffId, hackathonId, submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found"));
    }

    // Registra o aggiorna la valutazione di una submission.
    @PostMapping("/hackathons/{hackathonId}/submissions/{submissionId}/evaluation")
    public EvaluationResponse evaluateSubmission(
            @PathVariable long hackathonId,
            @PathVariable long submissionId,
            @RequestBody EvaluateRequest request,
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        long staffId = SessionAuth.requireStaffId(sessionStore, token);
        ValidateInput validateInput = validateInput(request);

        // Prima autorizzazione judge+stato, poi controllo che la submission sia dell'hackathon.
        evaluationController.assertHackathonInReviewForJudge(staffId, hackathonId);
        ensureSubmissionBelongsToHackathon(staffId, hackathonId, submissionId);

        return EvaluationResponse.from(evaluationController.evaluateSubmission(
                staffId,
                submissionId,
                validateInput.score(),
                validateInput.comment()
        ));
    }

    // Recupera la valutazione corrente di una submission.
    @GetMapping("/hackathons/{hackathonId}/submissions/{submissionId}/evaluation")
    public ResponseEntity<EvaluationResponse> getEvaluation(
            @PathVariable long hackathonId,
            @PathVariable long submissionId,
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        long staffId = SessionAuth.requireStaffId(sessionStore, token);
        ensureSubmissionBelongsToHackathon(staffId, hackathonId, submissionId);

        return evaluationController.viewEvaluation(staffId, submissionId)
                .map(EvaluationResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Crea un hackathon nuovo come organizer.
    @PostMapping("/organizer/hackathons")
    public ResponseEntity<Map<String, Long>> createHackathon(
            @RequestBody CreateHackathonRequest request,
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        long staffId = SessionAuth.requireStaffId(sessionStore, token);

        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Nome hackathon non valido");
        }
        // Controlli rapidi sul payload prima di passare al dominio.
        if (request.judgeId() <= 0) {
            throw new IllegalArgumentException("Judge non valido");
        }
        if (request.mentorIds() == null || request.mentorIds().isEmpty()) {
            throw new IllegalArgumentException("Almeno un mentor e richiesto");
        }
        if (request.mentorIds().stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("Mentor non valido");
        }
        if (request.prizeAmount() == null) {
            throw new IllegalArgumentException("Premio non valido");
        }

        Hackathon created = hackathonController.createHackathon(
                staffId,
                request.name(),
                request.regulation(),
                request.registrationDeadline(),
                request.startDate(),
                request.endDate(),
                request.submissionDeadline(),
                request.location(),
                request.prizeAmount(),
                request.maxTeamSize(),
                request.judgeId(),
                request.mentorIds()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("hackathonId", created.getHackathonId()));
    }

    @PutMapping("/organizer/hackathons/{hackathonId}")
    public Hackathon updateHackathon(@RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token,
                                     @PathVariable long hackathonId, @RequestBody CreateHackathonRequest request) {
        if (request == null) throw new IllegalArgumentException("Dati obbligatori");
        return hackathonController.updateHackathon(SessionAuth.requireStaffId(sessionStore, token), hackathonId,
                request.name(), request.regulation(), request.registrationDeadline(), request.startDate(), request.endDate(),
                request.submissionDeadline(), request.location(), request.prizeAmount(), request.maxTeamSize());
    }

    @DeleteMapping("/organizer/hackathons/{hackathonId}")
    public void deleteHackathon(@RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token,
                                @PathVariable long hackathonId) {
        hackathonController.deleteHackathon(SessionAuth.requireStaffId(sessionStore, token), hackathonId);
    }

    @PostMapping(value = "/mentor/reports/{reportId}/update", consumes = "application/x-www-form-urlencoded")
    public ViolationReport updateReport(@RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token,
                                        @PathVariable long reportId, @RequestParam String description) {
        return violationReportController.updateReport(SessionAuth.requireStaffId(sessionStore, token), reportId, description);
    }

    @PostMapping("/mentor/reports/{reportId}/withdraw")
    public void withdrawReport(@RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token,
                                @PathVariable long reportId) {
        violationReportController.withdrawReport(SessionAuth.requireStaffId(sessionStore, token), reportId);
    }

    @GetMapping("/judge/hackathons/{hackathonId}/expulsions")
    public List<ViolationReport> listExpulsions(@RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token,
                                               @PathVariable long hackathonId) {
        return violationReportController.listExpulsions(SessionAuth.requireStaffId(sessionStore, token), hackathonId);
    }

    // Avanza lo stato dell'hackathon.
    @PostMapping("/organizer/hackathons/{hackathonId}/advance")
    public Map<String, String> advanceHackathon(
            @PathVariable long hackathonId,
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        long staffId = SessionAuth.requireStaffId(sessionStore, token);
        hackathonController.advanceStatus(staffId, hackathonId);
        return Map.of("status", "ok");
    }

    // Aggiunge mentor all'hackathon.
    @PostMapping("/organizer/hackathons/{hackathonId}/mentors")
    public Map<String, String> addMentors(
            @PathVariable long hackathonId,
            @RequestBody AddMentorsRequest request,
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        long staffId = SessionAuth.requireStaffId(sessionStore, token);
        List<Long> mentorIds = request == null ? null : request.mentorIds();
        if (mentorIds == null || mentorIds.isEmpty()) {
            throw new IllegalArgumentException("Seleziona almeno un mentor");
        }
        if (mentorIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("Mentor non valido");
        }

        mentorManagementController.addMentors(staffId, hackathonId, mentorIds);
        return Map.of("status", "ok");
    }

    // Imposta il winner dell'hackathon.
    @PostMapping("/organizer/hackathons/{hackathonId}/winner")
    public Map<String, String> setWinner(
            @PathVariable long hackathonId,
            @RequestBody SetWinnerRequest request,
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        long staffId = SessionAuth.requireStaffId(sessionStore, token);
        if (request == null || request.teamId() <= 0) {
            throw new IllegalArgumentException("Team non valido");
        }

        hackathonController.setWinner(staffId, hackathonId, request.teamId());
        return Map.of("status", "ok");
    }

    // Tenta il pagamento del premio al winner.
    @PostMapping("/organizer/hackathons/{hackathonId}/pay-prize")
    public Map<String, String> payPrize(
            @PathVariable long hackathonId,
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        long staffId = SessionAuth.requireStaffId(sessionStore, token);
        String result = hackathonController.payPrize(staffId, hackathonId);

        Map<String, String> response = new LinkedHashMap<>();
        response.put("result", result);

        // Espone receipt/error solo se presenti.
        String receiptId = hackathonController.getHackathonDetails(hackathonId).orElseThrow().getPrizeReceiptId();
        if (receiptId != null && !receiptId.isBlank()) {
            response.put("receiptId", receiptId);
        }

        String lastErrorMessage = paymentSystem.getLastErrorMessage();
        if (result.contains("fallito") && lastErrorMessage != null && !lastErrorMessage.isBlank()) {
            response.put("lastErrorMessage", lastErrorMessage);
        }
        return response;
    }

    // Elenca gli hackathon in cui lo staff e' mentor.
    @GetMapping("/mentor/hackathons")
    public List<SupportController.MentorHackathonView> listMentorHackathons(
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        long staffId = SessionAuth.requireStaffId(sessionStore, token);
        return supportController.listMentorAssignedHackathons(staffId);
    }

    // Elenca le richieste supporto visibili al mentor.
    @GetMapping("/mentor/hackathons/{hackathonId}/support-requests")
    public List<SupportController.SupportRequestView> listSupportRequests(
            @PathVariable long hackathonId,
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        long staffId = SessionAuth.requireStaffId(sessionStore, token);
        return supportController.listSupportRequestsForMentor(staffId, hackathonId);
    }

    // Crea una proposta call per una richiesta supporto.
    @PostMapping("/mentor/support-requests/{requestId}/call-proposals")
    public ResponseEntity<CallProposalDto> createCallProposal(
            @PathVariable long requestId,
            @RequestBody CreateCallProposalRequest request,
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        if (requestId <= 0) {
            throw new IllegalArgumentException("Support request non valida");
        }
        long staffId = SessionAuth.requireStaffId(sessionStore, token);
        if (request == null || request.proposedStart() == null || request.proposedEnd() == null) {
            throw new IllegalArgumentException("Intervallo proposta non valido");
        }
        if (!request.proposedStart().isBefore(request.proposedEnd())) {
            throw new IllegalArgumentException("Intervallo proposta non valido");
        }

        CallProposal proposal = supportController.createCallProposal(
                staffId,
                requestId,
                request.proposedStart(),
                request.proposedEnd()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(CallProposalDto.from(proposal));
    }

    // Apre una segnalazione disciplinare come mentor.
    @PostMapping("/mentor/violation-reports")
    public ResponseEntity<Map<String, Long>> createViolationReport(
            @RequestBody CreateViolationReportRequest request,
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        long staffId = SessionAuth.requireStaffId(sessionStore, token);
        if (request == null || request.hackathonId() <= 0 || request.teamId() <= 0) {
            throw new IllegalArgumentException("Dati report non validi");
        }
        String description = request.description();
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Descrizione non valida");
        }

        ViolationReport report = violationReportController.createReport(
                staffId,
                request.hackathonId(),
                request.teamId(),
                description.trim()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("reportId", report.getReportId()));
    }

    // Mostra le segnalazioni ancora pendenti.
    @GetMapping("/organizer/hackathons/{hackathonId}/violation-reports/pending")
    public List<ViolationReportDto> listPendingReports(
            @PathVariable long hackathonId,
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        long staffId = SessionAuth.requireStaffId(sessionStore, token);
        return violationReportController.listReports(staffId, hackathonId, true).stream()
                .map(ViolationReportDto::from)
                .toList();
    }

    // Applica la decisione organizer sul report.
    @PostMapping("/organizer/violation-reports/{reportId}/decision")
    public Map<String, String> manageReport(
            @PathVariable long reportId,
            @RequestBody ManageViolationReportRequest request,
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        long staffId = SessionAuth.requireStaffId(sessionStore, token);
        String decision = normalizeDecision(request == null ? null : request.decision());
        violationReportController.manageReport(staffId, reportId, decision);
        return Map.of("status", "ok");
    }

    @GetMapping("/judge/hackathons/{hackathonId}/warnings")
    public List<it.unicam.hackhub.model.ViolationReport> listWarnings(
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token,
            @PathVariable long hackathonId) {
        return violationReportController.listWarnings(SessionAuth.requireStaffId(sessionStore, token), hackathonId);
    }

    @PostMapping("/judge/warnings/{reportId}/evaluation")
    public it.unicam.hackhub.model.ViolationReport evaluateWarning(
            @PathVariable long reportId, @RequestBody WarningEvaluationRequest request,
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        long staffId = SessionAuth.requireStaffId(sessionStore, token);
        if (request == null || request.penalty() == null) throw new IllegalArgumentException("Penalità obbligatoria");
        return evaluationController.evaluateWarning(staffId, reportId, request.penalty());
    }

    private record WarningEvaluationRequest(Integer penalty) { }

    // Controlla che la submission appartenga davvero all'hackathon richiesto.
    private void ensureSubmissionBelongsToHackathon(long staffId, long hackathonId, long submissionId) {
        submissionAccessController.getSubmissionDetailForHackathon(staffId, hackathonId, submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found in this hackathon"));
    }

    // Valida score/comment prima di passare al controller dominio.
    private ValidateInput validateInput(EvaluateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dati valutazione non validi");
        }
        if (request.score() < 0 || request.score() > 30) {
            throw new IllegalArgumentException("Score non valido");
        }
        String comment = request.comment();
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("Commento non valido");
        }
        return new ValidateInput(request.score(), comment.trim());
    }

    private LocalDate toDate(java.time.LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalDate();
    }

    // Accetta solo decisioni REJECT o EXPEL.
    private String normalizeDecision(String decision) {
        if (decision == null || decision.isBlank()) {
            throw new IllegalArgumentException("Decision non valida");
        }

        String normalized = decision.trim().toUpperCase(Locale.ROOT);
        if (!"REJECT".equals(normalized) && !"EXPEL".equals(normalized) && !"WARN".equals(normalized)) {
            throw new IllegalArgumentException("Decision non valida");
        }
        return normalized;
    }

    private record ValidateInput(int score, String comment) {
    }

    private record AddMentorsRequest(List<Long> mentorIds) {
    }

    private record EvaluateRequest(int score, String comment) {
    }

    private record EvaluationResponse(
            long evaluationId,
            long submissionId,
            int score,
            int baseScore,
            int penalty,
            LocalDateTime evaluatedAt
    ) {
        private static EvaluationResponse from(Evaluation evaluation) {
            return new EvaluationResponse(
                    evaluation.getEvaluationId(),
                    evaluation.getSubmissionId(),
                    evaluation.getScore(),
                    evaluation.getBaseScore(),
                    evaluation.getPenalty(),
                    evaluation.getEvaluatedAt()
            );
        }

        private static EvaluationResponse from(EvaluationController.EvaluationView evaluationView) {
            return new EvaluationResponse(
                    evaluationView.evaluationId(),
                    evaluationView.submissionId(),
                    evaluationView.score(),
                    evaluationView.baseScore(),
                    evaluationView.penalty(),
                    evaluationView.evaluatedAt()
            );
        }
    }

    private record SetWinnerRequest(long teamId) {
    }

    private record StaffMemberDto(long staffId, String username, String name) {
        private static StaffMemberDto from(StaffMember staffMember) {
            return new StaffMemberDto(
                    staffMember.getStaffId(),
                    staffMember.getStaffUsername(),
                    staffMember.getStaffName()
            );
        }
    }

    private record CreateCallProposalRequest(LocalDateTime proposedStart, LocalDateTime proposedEnd) {
    }

    private record CallProposalDto(
            long proposalId,
            long supportRequestId,
            LocalDateTime proposedStart,
            LocalDateTime proposedEnd,
            boolean booked
    ) {
        private static CallProposalDto from(CallProposal callProposal) {
            return new CallProposalDto(
                    callProposal.getProposalId(),
                    callProposal.getRequestId(),
                    callProposal.getProposedStart(),
                    callProposal.getProposedEnd(),
                    callProposal.isBooked()
            );
        }
    }

    private record CreateViolationReportRequest(long hackathonId, long teamId, String description) {
    }

    private record ViolationReportDto(
            long reportId,
            long hackathonId,
            long teamId,
            long mentorStaffId,
            String description,
            LocalDateTime createdAt,
            String decision
    ) {
        private static ViolationReportDto from(ViolationReport report) {
            return new ViolationReportDto(
                    report.getReportId(),
                    report.getHackathonId(),
                    report.getTeamId(),
                    report.getMentorStaffId(),
                    report.getDescription(),
                    report.getCreatedAt(),
                    report.getDecision()
            );
        }
    }

    private record ManageViolationReportRequest(String decision) {
    }
}
