package it.unicam.hackhub.controller;

import it.unicam.hackhub.external.PaymentSystem;
import it.unicam.hackhub.model.Evaluation;
import it.unicam.hackhub.model.Hackathon;
import it.unicam.hackhub.model.StaffAssignment;
import it.unicam.hackhub.model.StaffMember;
import it.unicam.hackhub.model.Submission;
import it.unicam.hackhub.model.Team;
import it.unicam.hackhub.model.TeamRegistration;
import it.unicam.hackhub.model.ViolationReport;
import it.unicam.hackhub.model.enums.HackathonStatus;
import it.unicam.hackhub.model.enums.StaffRole;
import it.unicam.hackhub.repository.EvaluationRepository;
import it.unicam.hackhub.repository.HackathonRepository;
import it.unicam.hackhub.repository.StaffAssignmentRepository;
import it.unicam.hackhub.repository.StaffMemberRepository;
import it.unicam.hackhub.repository.SubmissionRepository;
import it.unicam.hackhub.repository.TeamRegistrationRepository;
import it.unicam.hackhub.repository.TeamRepository;

import java.math.BigDecimal;
import it.unicam.hackhub.repository.UserRepository;
import it.unicam.hackhub.model.User;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class HackathonController {
    private final HackathonRepository hackathonRepository;
    private final StaffAssignmentRepository staffAssignmentRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final TeamRegistrationRepository teamRegistrationRepository;
    private final SubmissionRepository submissionRepository;
    private final EvaluationRepository evaluationRepository;
    private final TeamRepository teamRepository;
    private final PaymentSystem paymentSystem;
    private final it.unicam.hackhub.repository.ViolationReportRepository violationReportRepository;

    private final UserRepository userRepository;

    public HackathonController(HackathonRepository hackathonRepository,
                               StaffAssignmentRepository staffAssignmentRepository,
                               StaffMemberRepository staffMemberRepository,
                               TeamRegistrationRepository teamRegistrationRepository,
                               SubmissionRepository submissionRepository,
                               EvaluationRepository evaluationRepository,
                               TeamRepository teamRepository,
                               PaymentSystem paymentSystem,
                               it.unicam.hackhub.repository.ViolationReportRepository violationReportRepository, UserRepository userRepository) {
        this.userRepository = userRepository;
        this.hackathonRepository = hackathonRepository;
        this.staffAssignmentRepository = staffAssignmentRepository;
        this.staffMemberRepository = staffMemberRepository;
        this.teamRegistrationRepository = teamRegistrationRepository;
        this.submissionRepository = submissionRepository;
        this.evaluationRepository = evaluationRepository;
        this.teamRepository = teamRepository;
        this.paymentSystem = paymentSystem;
        this.violationReportRepository = violationReportRepository;
    }

    // Restituisce tutti gli hackathon.
    public List<Hackathon> listHackathons() {
        return hackathonRepository.findAll();
    }

    // Restituisce il dettaglio di un hackathon.
    public Optional<Hackathon> getHackathonDetails(long hackathonId) {
        return hackathonRepository.findById(hackathonId);
    }

    // Elenca lo staff selezionabile durante la creazione hackathon.
    public List<StaffSelectView> listSelectableStaff(long currentStaffId) {
        if (currentStaffId <= 0) {
            throw new IllegalArgumentException("Staff non valido");
        }
        ensureCanCreateHackathon(currentStaffId);

        List<StaffSelectView> result = new ArrayList<>();
        for (StaffMember staffMember : staffMemberRepository.findAll()) {
            result.add(new StaffSelectView(
                    staffMember.getStaffId(),
                    safe(staffMember.getStaffUsername()),
                    safe(staffMember.getStaffName())
            ));
        }
        return result;
    }

    // Elenca lo staff selezionabile filtrato per ruolo.
    public List<StaffSelectView> listSelectableStaffByRole(long currentStaffId, StaffRole role) {
        if (currentStaffId <= 0) {
            throw new IllegalArgumentException("Staff non valido");
        }
        if (role == null) {
            throw new IllegalArgumentException("Ruolo non valido");
        }
        ensureCanCreateHackathon(currentStaffId);

        List<StaffSelectView> result = new ArrayList<>();
        for (StaffMember staffMember : staffMemberRepository.findAll()) {
            long staffId = staffMember.getStaffId();
            result.add(new StaffSelectView(
                    staffId,
                    safe(staffMember.getStaffUsername()),
                    safe(staffMember.getStaffName())
            ));
        }
        return result;
    }

    // Crea un hackathon nuovo con staff iniziale organizer/judge/mentor.
    @org.springframework.transaction.annotation.Transactional
    public Hackathon createHackathon(long currentStaffId,
                                     String name,
                                     String regulation,
                                     LocalDateTime regDeadline,
                                     LocalDateTime startDate,
                                     LocalDateTime endDate,
                                     LocalDateTime submissionDeadline,
                                     String location,
                                     BigDecimal prizeAmount,
                                     int maxTeamSize,
                                     long judgeStaffId,
                                     List<Long> mentorStaffIds) {
        if (currentStaffId <= 0) {
            throw new IllegalArgumentException("Staff non valido");
        }
        ensureCanCreateHackathon(currentStaffId);
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome hackathon non valido");
        }
        String normalizedName = name.trim();
        if (hackathonRepository.findByName(normalizedName).isPresent()) {
            throw new IllegalArgumentException("Nome hackathon gia esistente");
        }
        if (location == null || location.trim().isEmpty()) {
            throw new IllegalArgumentException("Location non valida");
        }
        if (maxTeamSize <= 0) {
            throw new IllegalArgumentException("Max team size non valido");
        }
        if (prizeAmount == null || prizeAmount.signum() < 0) {
            throw new IllegalArgumentException("Premio non valido");
        }

        // Le date devono essere coerenti prima di creare l'hackathon.
        if (regDeadline == null || startDate == null || endDate == null) {
            throw new IllegalArgumentException("Date hackathon non valide");
        }
        if (!regDeadline.isBefore(startDate)) {
            throw new IllegalArgumentException("Registration deadline deve essere prima dello start date");
        }
        if (!startDate.isBefore(endDate)) {
            throw new IllegalArgumentException("Start date deve essere prima di end date");
        }
        if (submissionDeadline != null && !submissionDeadline.equals(endDate)) {
            throw new IllegalArgumentException("La scadenza delle sottomissioni coincide con la fine della gara");
        }
        if (mentorStaffIds == null || mentorStaffIds.isEmpty()) {
            throw new IllegalArgumentException("Almeno un mentor e richiesto");
        }
        if (judgeStaffId <= 0) throw new IllegalArgumentException("Judge non valido");
        // Judge e mentor devono avere davvero quel ruolo.

        Set<Long> mentorIds = new LinkedHashSet<>();
        for (Long mentorId : mentorStaffIds) {
            if (mentorId == null || mentorId <= 0) {
                throw new IllegalArgumentException("Mentor non valido");
            }
            mentorIds.add(mentorId);
        }
        if (mentorIds.isEmpty()) {
            throw new IllegalArgumentException("Almeno un mentor e richiesto");
        }
        staffMemberRepository.findById(judgeStaffId)
                .orElseThrow(() -> new IllegalArgumentException("Judge non trovato"));
        for (Long mentorId : mentorIds) {
            staffMemberRepository.findById(mentorId)
                    .orElseThrow(() -> new IllegalArgumentException("Mentor non trovato: " + mentorId));
        }

        if (!regDeadline.isAfter(LocalDateTime.now()))
            throw new IllegalArgumentException("Scadenze non valide");
        if (regulation == null || regulation.isBlank()) throw new IllegalArgumentException("Regolamento obbligatorio");

        Hackathon hackathon = Hackathon.builder()
                .hackathonId(0L)
                .hackathonName(normalizedName)
                .regulation(regulation == null ? "" : regulation.trim())
                .registrationDeadline(regDeadline)
                .startDate(startDate)
                .endDate(endDate)
                .submissionDeadline(endDate)
                .location(location.trim())
                .prizeAmount(prizeAmount)
                .maxTeamSize(maxTeamSize)
                .status(HackathonStatus.REGISTRATION)
                .winnerTeamId(null)
                .build();
        Hackathon savedHackathon = hackathonRepository.save(hackathon);

        // Le assegnazioni vengono salvate subito dopo la creazione.
        List<StaffAssignment> assignments = new ArrayList<>();
        assignments.add(new StaffAssignment(0L, currentStaffId, savedHackathon.getHackathonId(), StaffRole.ORGANIZER));
        assignments.add(new StaffAssignment(0L, judgeStaffId, savedHackathon.getHackathonId(), StaffRole.JUDGE));
        for (Long mentorId : mentorIds) {
            assignments.add(new StaffAssignment(0L, mentorId, savedHackathon.getHackathonId(), StaffRole.MENTOR));
        }
        for (StaffAssignment assignment : assignments) {
            staffAssignmentRepository.save(assignment);
        }

        return savedHackathon;
    }

    @org.springframework.transaction.annotation.Transactional
    public Hackathon updateHackathon(long currentStaffId, long hackathonId, String name, String regulation,
            LocalDateTime registrationDeadline, LocalDateTime startDate, LocalDateTime endDate,
            LocalDateTime submissionDeadline, String location, BigDecimal prizeAmount, int maxTeamSize) {
        Hackathon event = hackathonRepository.findById(hackathonId).orElseThrow(() -> new IllegalArgumentException("Hackathon non trovato"));
        ensureOrganizer(currentStaffId, hackathonId, "Solo l'organizzatore assegnato può modificare l'evento");
        LocalDateTime now = LocalDateTime.now();
        if (event.isDeleted() || !event.canRegister() || event.getRegistrationDeadline() == null
                || !now.isBefore(event.getRegistrationDeadline()) || !now.isBefore(event.getStartDate()))
            throw new IllegalStateException("Evento non più modificabile");
        if (name == null || name.isBlank() || regulation == null || regulation.isBlank()
                || location == null || location.isBlank() || prizeAmount == null || prizeAmount.signum() < 0
                || maxTeamSize <= 0 || registrationDeadline == null || startDate == null || endDate == null
                || !registrationDeadline.isAfter(now)
                || !registrationDeadline.isBefore(startDate) || !startDate.isBefore(endDate)
                || (submissionDeadline != null && !submissionDeadline.equals(endDate)))
            throw new IllegalArgumentException("Dati o scadenze non validi");
        if (hackathonRepository.findByName(name.trim()).filter(h -> h.getHackathonId() != hackathonId).isPresent())
            throw new IllegalArgumentException("Nome già utilizzato");
        for (TeamRegistration registration : teamRegistrationRepository.findByHackathonId(hackathonId)) {
            if (userRepository.findByTeamId(registration.getTeamId()).size() > maxTeamSize)
                throw new IllegalStateException("La capienza escluderebbe un team già iscritto");
        }
        event.setHackathonName(name.trim());
        event.setRegulation(regulation.trim());
        event.setRegistrationDeadline(registrationDeadline);
        event.setStartDate(startDate);
        event.setEndDate(endDate);
        event.setSubmissionDeadline(endDate);
        event.setLocation(location.trim());
        event.setPrizeAmount(prizeAmount);
        event.setMaxTeamSize(maxTeamSize);
        return hackathonRepository.save(event);
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteHackathon(long currentStaffId, long hackathonId) {
        Hackathon event = hackathonRepository.findById(hackathonId).orElseThrow(() -> new IllegalArgumentException("Hackathon non trovato"));
        ensureOrganizer(currentStaffId, hackathonId, "Solo l'organizzatore assegnato può eliminare l'evento");
        if (event.isDeleted() || !event.canRegister() || !LocalDateTime.now().isBefore(event.getStartDate())
                || !teamRegistrationRepository.findByHackathonId(hackathonId).isEmpty())
            throw new IllegalStateException("Eliminazione consentita solo prima dell'inizio e senza iscrizioni");
        event.setDeleted(true);
        hackathonRepository.save(event);
    }

    public PhaseTransition viewPhaseTransition(long currentStaffId, long hackathonId) {
        Hackathon event = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new IllegalArgumentException("Hackathon non trovato"));
        ensureOrganizer(currentStaffId, hackathonId, "Solo l’organizzatore può consultare le transizioni");
        return transitionFor(event);
    }

    private PhaseTransition transitionFor(Hackathon event) {
        return switch (event.getStatus()) {
            case REGISTRATION -> new PhaseTransition(event.getStatus(), HackathonStatus.RUNNING,
                    event.getStartDate(), "Inizio della gara");
            case RUNNING -> new PhaseTransition(event.getStatus(), HackathonStatus.REVIEW,
                    event.getEndDate(), "Fine della gara");
            case REVIEW -> new PhaseTransition(event.getStatus(), HackathonStatus.CLOSED,
                    null, "Vincitore proclamato e premio gestito");
            case CLOSED -> new PhaseTransition(event.getStatus(), null, null, "Evento concluso");
        };
    }

    public record PhaseTransition(HackathonStatus current, HackathonStatus next, LocalDateTime availableFrom,
                                  String requirement) {
    }

    // Porta l'hackathon allo stato successivo.
    @org.springframework.transaction.annotation.Transactional
    public Hackathon advanceStatus(long currentStaffId, long hackathonId) {
        return advanceStatus(currentStaffId, hackathonId, null);
    }

    @org.springframework.transaction.annotation.Transactional
    public Hackathon advanceStatus(long currentStaffId, long hackathonId, HackathonStatus expectedNext) {
        Hackathon hackathon = hackathonRepository.findByIdForUpdate(hackathonId)
                .orElseThrow(() -> new IllegalArgumentException("Hackathon not found"));
        ensureOrganizer(currentStaffId, hackathonId, "Not authorized: only organizer can advance status");

        HackathonStatus nextStatus = transitionFor(hackathon).next();
        if (nextStatus == null) {
            throw new IllegalStateException("Hackathon già concluso");
        }
        if (expectedNext != null && expectedNext != nextStatus) {
            throw new IllegalStateException("La prossima fase disponibile è " + nextStatus);
        }

        if (nextStatus == HackathonStatus.CLOSED && (hackathon.getWinnerTeamId() == null || !hackathon.isPrizePaid())) {
            throw new IllegalStateException("Concludere prima proclamazione e pagamento del premio");
        }
        LocalDateTime now = LocalDateTime.now();
        if (hackathon.isDeleted()) throw new IllegalStateException("Evento eliminato");
        if (nextStatus == HackathonStatus.RUNNING) {
            if (now.isBefore(hackathon.getStartDate())) throw new IllegalStateException("La gara non è ancora iniziata");
            for (TeamRegistration registration : teamRegistrationRepository.findByHackathonId(hackathonId)) {
                registration.freezeParticipants(userRepository.findByTeamId(registration.getTeamId()).stream().map(User::getUserId).toList());
                teamRegistrationRepository.save(registration);
            }
        }
        if (nextStatus == HackathonStatus.REVIEW && now.isBefore(hackathon.getEndDate()))
            throw new IllegalStateException("La gara non è ancora terminata");
        hackathon.changeStatus(nextStatus);
        return hackathonRepository.save(hackathon);
    }

    public ProclamationView viewProclamation(long currentStaffId, long hackathonId) {
        ensureOrganizer(currentStaffId, hackathonId, "Solo l’organizzatore può consultare la proclamazione");
        Hackathon event = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new IllegalArgumentException("Hackathon non trovato"));
        List<ViolationReport> reports = violationReportRepository.findByHackathonId(hackathonId);
        List<ProclamationTeam> teams = teamRegistrationRepository.findByHackathonId(hackathonId).stream()
                .map(registration -> proclamationTeam(registration, reports))
                .toList();
        boolean missing = teams.stream().anyMatch(ProclamationTeam::missingRequirements);
        boolean hasCandidate = teams.stream().anyMatch(ProclamationTeam::isEligible);
        return new ProclamationView(event.getStatus(), event.getWinnerTeamId(), event.isPrizePaid(),
                event.getPrizeReceiptId(), !missing && hasCandidate && event.canEvaluate() && event.getWinnerTeamId() == null,
                teams);
    }

    private ProclamationTeam proclamationTeam(TeamRegistration registration, List<ViolationReport> reports) {
        long teamId = registration.getTeamId();
        String teamName = teamRepository.findById(teamId).map(Team::getTeamName).orElse(null);
        Optional<Submission> submission = submissionRepository.findByRegistrationId(registration.getRegistrationId());
        Optional<Evaluation> evaluation = submission.flatMap(item -> evaluationRepository.findBySubmissionId(item.getSubmissionId()));
        List<Long> pendingWarnings = reports.stream()
                .filter(report -> report.getTeamId() == teamId && "TEAM_WARNED".equals(report.getDecision())
                        && !report.isPenaltyEvaluated())
                .map(ViolationReport::getReportId).toList();
        return new ProclamationTeam(teamId, teamName, registration.isExpelled(),
                submission.map(Submission::getSubmissionId).orElse(null), evaluation.map(Evaluation::getScore).orElse(null),
                pendingWarnings);
    }

    public record ProclamationTeam(long teamId, String teamName, boolean expelled, Long submissionId,
                                   Integer finalScore, List<Long> pendingWarnings) {
        public boolean isEligible() {
            return !expelled && submissionId != null && finalScore != null;
        }

        public boolean missingRequirements() {
            return !expelled && submissionId != null && (finalScore == null || !pendingWarnings.isEmpty());
        }
    }

    public record ProclamationView(HackathonStatus status, Long winnerTeamId, boolean prizePaid, String receiptId,
                                   boolean canProclaim, List<ProclamationTeam> teams) {
    }

    public Optional<String> getWinnerTeamName(Hackathon hackathon) {
        return Optional.ofNullable(hackathon.getWinnerTeamId())
                .flatMap(teamRepository::findById)
                .map(Team::getTeamName);
    }

    // La proclamazione viene salvata indipendentemente dall'erogazione del premio.
    @org.springframework.transaction.annotation.Transactional
    public Hackathon setWinner(long currentStaffId, long hackathonId, long teamId) {
        Hackathon hackathon = hackathonRepository.findByIdForUpdate(hackathonId)
                .orElseThrow(() -> new IllegalArgumentException("Hackathon not found"));
        ensureOrganizer(currentStaffId, hackathonId, "Not authorized: only organizer can manage prize");

        Long existingWinnerTeamId = hackathon.getWinnerTeamId();
        // Ripetere la stessa proclamazione non modifica il risultato né avvia pagamenti.
        if (existingWinnerTeamId != null) {
            if (existingWinnerTeamId == teamId) {
                return hackathon;
            }
            if (existingWinnerTeamId != teamId) {
                throw new IllegalStateException("Winner already set to another team");
            }
        }

        if (!hackathon.canEvaluate()) {
            throw new IllegalStateException("Hackathon not in REVIEW");
        }

        // Il winner deve essere un team registrato e non espulso.
        TeamRegistration registration = teamRegistrationRepository
                .findByTeamIdAndHackathonId(teamId, hackathonId)
                .orElseThrow(() -> new IllegalArgumentException("Team not registered to this hackathon"));
        if (registration.isExpelled()) {
            throw new IllegalStateException("Cannot set winner for expelled team");
        }

        Submission winnerSubmission = submissionRepository.findByRegistrationId(registration.getRegistrationId())
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot set winner: selected team has no submission"
                ));
        evaluationRepository.findBySubmissionId(winnerSubmission.getSubmissionId())
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot set winner: selected team submission not evaluated"
                ));

        if (violationReportRepository.findByHackathonId(hackathonId).stream()
                .anyMatch(r -> "TEAM_WARNED".equals(r.getDecision()) && !r.isPenaltyEvaluated()
                        && teamRegistrationRepository.findByTeamIdAndHackathonId(r.getTeamId(), hackathonId)
                        .map(reg -> !reg.isExpelled() && submissionRepository.findByRegistrationId(reg.getRegistrationId()).isPresent()).orElse(false))) {
            throw new IllegalStateException("Valutare tutte le ammonizioni prima della proclamazione");
        }
        // Tutte le consegne ricevute dai team eleggibili devono essere valutate.
        ensureAllTeamsEvaluated(hackathonId);

        teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalStateException("Winner team not found"));
        hackathon.setWinnerTeamId(teamId);
        return hackathonRepository.save(hackathon);
    }

    // Eroga il premio se il winner e' gia' stato deciso.
    @org.springframework.transaction.annotation.Transactional
    public String payPrize(long currentStaffId, long hackathonId) {
        return payPrizeResult(currentStaffId, hackathonId).result();
    }

    @org.springframework.transaction.annotation.Transactional
    public synchronized PrizeResult payPrizeResult(long currentStaffId, long hackathonId) {
        Hackathon hackathon = hackathonRepository.findByIdForUpdate(hackathonId)
                .orElseThrow(() -> new IllegalArgumentException("Hackathon not found"));
        ensureOrganizer(currentStaffId, hackathonId, "Not authorized: only organizer can manage prize");

        if (hackathon.isPrizePaid()) {
            return prizeResult(hackathon, "Premio gia erogato: receipt=" + hackathon.getPrizeReceiptId()
                    + ", teamId=" + hackathon.getWinnerTeamId() + ", amount=" + hackathon.getPrizeAmount(), null);
        }
        // REVIEW è il normale pagamento; CLOSED permette il recupero di eventi storici non pagati.
        if (hackathon.getStatus() != HackathonStatus.REVIEW && !hackathon.isClosed()) {
            throw new IllegalStateException("Pagamento consentito solo in REVIEW o CLOSED");
        }
        Long winnerTeamId = hackathon.getWinnerTeamId();
        if (winnerTeamId == null) {
            throw new IllegalStateException("Winner not set");
        }

        teamRegistrationRepository.findByTeamIdAndHackathonId(winnerTeamId, hackathonId)
                .orElseThrow(() -> new IllegalStateException("Winner team not registered to this hackathon"));
        if (hackathon.getPrizeAmount() == null) {
            throw new IllegalStateException("Prize amount not configured");
        }

        Team winnerTeam = teamRepository.findById(winnerTeamId)
                .orElseThrow(() -> new IllegalStateException("Winner team not found"));
        String winnerTeamName = winnerTeam.getTeamName();
        if (winnerTeamName == null || winnerTeamName.trim().isEmpty()) {
            throw new IllegalStateException("Winner team name not configured");
        }

        if (hackathon.getPrizeAmount().signum() < 0) throw new IllegalStateException("Premio non valido");
        boolean zeroPrize = hackathon.getPrizeAmount().signum() == 0;
        boolean paid = zeroPrize;
        String receipt = zeroPrize ? "ZERO-" + hackathonId : null;
        String error = null;
        if (!zeroPrize) {
            try {
                paid = paymentSystem.payPrize(hackathonId, hackathon.getPrizeAmount(), winnerTeamName);
                if (paid) {
                    receipt = paymentSystem.getLastReceiptId();
                } else {
                    error = paymentSystem.getLastErrorMessage();
                }
            } catch (RuntimeException ex) {
                throw new PrizeServiceUnavailableException(winnerTeamId, ex);
            }
        }

        if (paid) {
            hackathon.markPrizePaid();
            hackathon.setPrizeReceiptId(receipt);
            if (!hackathon.isClosed()) {
                hackathon.changeStatus(HackathonStatus.CLOSED);
            }
            hackathonRepository.save(hackathon);
            return prizeResult(hackathon, "Pagamento premio completato: receipt=" + receipt + ", teamId=" + winnerTeamId
                    + ", teamName=" + winnerTeamName + ", amount=" + hackathon.getPrizeAmount(), null);
        }
        return prizeResult(hackathon, "Pagamento premio fallito: teamId=" + winnerTeamId + ", teamName=" + winnerTeamName
                + ", amount=" + hackathon.getPrizeAmount(), error);
    }

    private PrizeResult prizeResult(Hackathon event, String message, String error) {
        return new PrizeResult(message, event.getWinnerTeamId(), event.isPrizePaid(), event.getPrizeReceiptId(),
                event.getStatus(), error);
    }

    public record PrizeResult(String result, Long winnerTeamId, boolean prizePaid, String receiptId,
                              HackathonStatus phase, String lastErrorMessage) {
    }

    public static class PrizeServiceUnavailableException extends IllegalStateException {
        private final long winnerTeamId;

        public PrizeServiceUnavailableException(long winnerTeamId, RuntimeException cause) {
            super("Servizio di pagamento indisponibile. Il premio resta da erogare.", cause);
            this.winnerTeamId = winnerTeamId;
        }

        public long winnerTeamId() {
            return winnerTeamId;
        }
    }

    // Verifica che lo staff sia organizer dell'hackathon.
    private void ensureOrganizer(long currentStaffId, long hackathonId, String notAuthorizedMessage) {
        boolean isOrganizer = staffAssignmentRepository.findByHackathonIdAndRole(hackathonId, StaffRole.ORGANIZER)
                .stream()
                .anyMatch(assignment -> assignment.getStaffId() == currentStaffId);
        if (!isOrganizer) {
            throw new IllegalArgumentException(notAuthorizedMessage);
        }
    }

    // Prima di proclamare il winner controlla che tutti i team attivi siano valutati.
    private void ensureAllTeamsEvaluated(long hackathonId) {
        List<TeamRegistration> registrations = teamRegistrationRepository.findByHackathonId(hackathonId);
        for (TeamRegistration registration : registrations) {
            if (registration.isExpelled()) {
                continue;
            }
            Optional<Submission> received = submissionRepository.findByRegistrationId(registration.getRegistrationId());
            if (received.isEmpty()) continue;
            Submission submission = received.get();
            Evaluation evaluation = evaluationRepository.findBySubmissionId(submission.getSubmissionId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Cannot set winner: submission " + submission.getSubmissionId() + " not evaluated"
                    ));
            if (evaluation.getEvaluationId() <= 0) {
                throw new IllegalStateException(
                        "Cannot set winner: submission " + submission.getSubmissionId() + " not evaluated"
                );
            }
        }
    }

    // Permesso creazione: organizer gia' assegnato o caso iniziale senza organizer.
    private void ensureCanCreateHackathon(long currentStaffId) {
        staffMemberRepository.findById(currentStaffId)
                .orElseThrow(() -> new IllegalArgumentException("Organizer non trovato"));

        boolean isOrganizer = staffAssignmentRepository.findByStaffId(currentStaffId).stream()
                .anyMatch(assignment -> assignment.getRole() == StaffRole.ORGANIZER);
        if (isOrganizer) {
            return;
        }

        throw new IllegalArgumentException("Not authorized: only organizer can create hackathon");
    }

    // Controlla se lo staff ha almeno un'assegnazione con quel ruolo.
    private boolean hasAnyAssignmentWithRole(long staffId, StaffRole role) {
        return staffAssignmentRepository.findByStaffId(staffId).stream()
                .anyMatch(assignment -> assignment.getRole() == role);
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }

    public record StaffSelectView(long staffId, String username, String name) {
    }
}
