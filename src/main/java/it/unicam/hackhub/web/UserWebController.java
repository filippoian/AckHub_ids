package it.unicam.hackhub.web;

import it.unicam.hackhub.controller.HackathonController;
import it.unicam.hackhub.controller.AuthController;
import it.unicam.hackhub.controller.UserProfile;
import org.springframework.web.bind.annotation.RequestParam;
import it.unicam.hackhub.controller.SubmissionController;
import it.unicam.hackhub.controller.SupportController;
import it.unicam.hackhub.controller.TeamController;
import it.unicam.hackhub.controller.TeamRegistrationController;
import it.unicam.hackhub.model.CallBooking;
import it.unicam.hackhub.model.CallProposal;
import it.unicam.hackhub.model.Hackathon;
import it.unicam.hackhub.model.Invitation;
import it.unicam.hackhub.model.Submission;
import it.unicam.hackhub.model.SupportRequest;
import it.unicam.hackhub.model.Team;
import it.unicam.hackhub.model.TeamRegistration;
import it.unicam.hackhub.model.User;
import it.unicam.hackhub.repository.CallProposalRepository;
import it.unicam.hackhub.repository.SupportRequestRepository;
import it.unicam.hackhub.repository.TeamRepository;
import it.unicam.hackhub.repository.UserRepository;
import it.unicam.hackhub.web.dto.CreateTeamRequest;
import it.unicam.hackhub.web.session.InMemorySessionStore;
import it.unicam.hackhub.web.session.SessionAuth;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/me")
public class UserWebController {
    private static final String SESSION_TOKEN_HEADER = "X-Session-Token";

    @org.springframework.beans.factory.annotation.Autowired
    private AuthController authController;
    private final TeamController teamController;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final HackathonController hackathonController;
    private final TeamRegistrationController teamRegistrationController;
    private final SubmissionController submissionController;
    private final SupportController supportController;
    private final SupportRequestRepository supportRequestRepository;
    private final CallProposalRepository callProposalRepository;
    private final InMemorySessionStore sessionStore;

    public UserWebController(TeamController teamController,
                             TeamRepository teamRepository,
                             UserRepository userRepository,
                             HackathonController hackathonController,
                             TeamRegistrationController teamRegistrationController,
                             SubmissionController submissionController,
                             SupportController supportController,
                             SupportRequestRepository supportRequestRepository,
                             CallProposalRepository callProposalRepository,
                             InMemorySessionStore sessionStore) {
        this.teamController = teamController;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.hackathonController = hackathonController;
        this.teamRegistrationController = teamRegistrationController;
        this.submissionController = submissionController;
        this.supportController = supportController;
        this.supportRequestRepository = supportRequestRepository;
        this.callProposalRepository = callProposalRepository;
        this.sessionStore = sessionStore;
    }

    @GetMapping("/profile")
    public UserProfile viewProfile(@RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        return authController.viewProfile(SessionAuth.requireUserId(sessionStore, token));
    }

    @PostMapping(value = "/profile", consumes = "application/x-www-form-urlencoded")
    public UserProfile updateProfile(@RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token,
            @RequestParam String userName, @RequestParam(required = false) String email,
            @RequestParam String currentPassword, @RequestParam(required = false) String newPassword) {
        return authController.updateProfile(SessionAuth.requireUserId(sessionStore, token), userName, email, currentPassword, newPassword);
    }

    @PostMapping(value = "/profile/deactivate", consumes = "application/x-www-form-urlencoded")
    public void deactivateProfile(@RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token,
                                  @RequestParam String currentPassword) {
        authController.deactivateProfile(SessionAuth.requireUserId(sessionStore, token), currentPassword);
    }

    @PostMapping(value = "/profile/recovery-code", consumes = "application/x-www-form-urlencoded")
    public Map<String, String> issueRecoveryCode(@RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token,
                                               @RequestParam String currentPassword) {
        return Map.of("recoveryCode", authController.issueRecoveryCode(SessionAuth.requireUserId(sessionStore, token), currentPassword));
    }

    @PostMapping("/support/requests/{requestId}/cancel")
    public void cancelSupportRequest(@RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token,
                                      @PathVariable long requestId) {
        supportController.cancelSupportRequest(SessionAuth.requireUserId(sessionStore, token), requestId);
    }

    // Crea un team per l'utente autenticato.
    @PostMapping("/team")
    public ResponseEntity<TeamDto> createTeam(
            @RequestBody CreateTeamRequest request,
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        long userId = SessionAuth.requireUserId(sessionStore, token);
        String teamName = request == null ? null : request.teamName();
        if (teamName == null || teamName.isBlank()) {
            throw new IllegalArgumentException("Nome team non valido");
        }

        Team createdTeam = teamController.createTeam(userId, teamName.trim());
        return ResponseEntity.status(HttpStatus.CREATED).body(TeamDto.from(createdTeam));
    }

    // Restituisce il team corrente dell'utente.
    @GetMapping("/team")
    public TeamDto getMyTeam(@RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        long userId = SessionAuth.requireUserId(sessionStore, token);
        Long teamId = teamController.getTeamIdOfUser(userId);
        if (teamId == null) {
            throw new IllegalArgumentException("Non hai un team");
        }

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team non trovato"));
        return TeamDto.from(team);
    }

    // Invia un invito dal team corrente a un altro utente.
    @PostMapping("/team/invitations")
    public ResponseEntity<Map<String, Long>> inviteUser(
            @RequestBody InviteUserRequest request,
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        long userId = SessionAuth.requireUserId(sessionStore, token);
        String invitedUsername = request == null ? null : request.invitedUsername();
        if (invitedUsername == null || invitedUsername.isBlank()) {
            throw new IllegalArgumentException("Username invitato non valido");
        }

        Long teamId = teamController.getTeamIdOfUser(userId);
        if (teamId == null) {
            throw new IllegalArgumentException("Non hai un team");
        }

        long invitedUserId = userRepository.findByUserName(invitedUsername.trim())
                .map(User::getUserId)
                .orElseThrow(() -> new IllegalArgumentException("Utente invitato non trovato"));

        Invitation invitation = teamController.inviteUser(userId, teamId, invitedUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("invitationId", invitation.getInvitationId()));
    }

    // Elenca gli inviti ricevuti.
    @GetMapping("/invitations")
    public List<InvitationDto> listInvitations(
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        long userId = SessionAuth.requireUserId(sessionStore, token);
        return teamController.viewInvites(userId).stream()
                .map(invitation -> new InvitationDto(
                        invitation.getInvitationId(),
                        invitation.getTeamId(),
                        teamRepository.findById(invitation.getTeamId())
                                .map(Team::getTeamName)
                                .orElse("N/A"),
                        invitation.getStatus() == null ? "UNKNOWN" : invitation.getStatus().name()
                ))
                .toList();
    }

    // Accetta o rifiuta un invito.
    @PostMapping("/invitations/{invitationId}")
    public Map<String, String> manageInvitation(
            @PathVariable long invitationId,
            @RequestBody ManageInvitationRequest request,
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        long userId = SessionAuth.requireUserId(sessionStore, token);
        String action = normalizeAction(request == null ? null : request.action());
        boolean done = switch (action) {
            case "ACCEPT" -> teamController.acceptInvitation(invitationId, userId);
            case "DECLINE" -> teamController.declineInvitation(invitationId, userId);
            default -> false;
        };

        if (!done) {
            throw new IllegalArgumentException("Invito non valido o non autorizzato");
        }
        return Map.of("status", "ok");
    }

    // Mostra gli hackathon ancora registrabili per il team.
    @GetMapping("/registerable-hackathons")
    public List<HackathonDto> listRegisterableHackathons(
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        long userId = SessionAuth.requireUserId(sessionStore, token);
        LocalDateTime now = LocalDateTime.now();

        return teamRegistrationController.listRegisterableHackathons(userId).stream()
                // Evita hackathon con deadline gia' passata.
                .filter(option -> option.registrationDeadline() == null
                        || !option.registrationDeadline().isBefore(now))
                .map(option -> hackathonController.getHackathonDetails(option.hackathonId()))
                .flatMap(Optional::stream)
                .map(HackathonDto::from)
                .toList();
    }

    // Registra il team a un hackathon.
    @PostMapping("/registration")
    public ResponseEntity<TeamRegistrationDto> registerTeam(
            @RequestBody RegisterTeamRequest request,
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        long userId = SessionAuth.requireUserId(sessionStore, token);
        if (request == null || request.hackathonId() <= 0) {
            throw new IllegalArgumentException("Hackathon non valido");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(
                TeamRegistrationDto.from(
                        teamRegistrationController.registerTeamToHackathon(userId, request.hackathonId())
                )
        );
    }

    // Restituisce la registrazione attuale del team.
    @GetMapping("/registration")
    public TeamRegistrationDto getMyRegistration(
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        long userId = SessionAuth.requireUserId(sessionStore, token);
        return teamRegistrationController.getMyRegistration(userId)
                .map(TeamRegistrationDto::from)
                .orElseThrow(() -> new IllegalArgumentException("Nessuna registrazione"));
    }

    // Recupera la submission del team, se presente.
    @GetMapping("/submission")
    public ResponseEntity<SubmissionDto> getMySubmission(
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        long userId = SessionAuth.requireUserId(sessionStore, token);
        long teamId = requireTeamId(userId);

        return submissionController.viewMySubmission(teamId)
                .map(SubmissionDto::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Invia la prima submission del team.
    @PostMapping("/submission")
    public SubmissionDto submit(
            @RequestBody SubmitRequest request,
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        long userId = SessionAuth.requireUserId(sessionStore, token);
        long teamId = requireTeamId(userId);
        String content = normalizeContent(request);

        // Qui consento solo il primo invio.
        if (submissionController.viewMySubmission(teamId).isPresent()) {
            throw new IllegalStateException("Submission gia inviata: usa update");
        }

        return SubmissionDto.from(submissionController.submitOrUpdate(userId, teamId, content));
    }

    // Aggiorna una submission gia' esistente.
    @PutMapping("/submission")
    public SubmissionDto update(
            @RequestBody SubmitRequest request,
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        long userId = SessionAuth.requireUserId(sessionStore, token);
        long teamId = requireTeamId(userId);
        String content = normalizeContent(request);

        // Qui invece e' obbligatorio che esista gia' una submission.
        if (submissionController.viewMySubmission(teamId).isEmpty()) {
            throw new IllegalStateException("Nessuna submission: usa submit");
        }

        return SubmissionDto.from(submissionController.submitOrUpdate(userId, teamId, content));
    }

    // Crea una richiesta supporto per il team corrente.
    @PostMapping("/support/requests")
    public ResponseEntity<SupportRequestDto> createSupportRequest(
            @RequestBody CreateSupportRequest request,
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        long userId = SessionAuth.requireUserId(sessionStore, token);
        if (request == null || request.message() == null || request.message().isBlank()) {
            throw new IllegalArgumentException("Messaggio non valido");
        }

        SupportRequest supportRequest = supportController.createSupportRequestForCurrentUser(
                userId,
                request.message().trim()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(SupportRequestDto.from(supportRequest));
    }

    // Elenca le richieste supporto gia' aperte dal team.
    @GetMapping("/support/requests")
    public List<SupportRequestDto> listMySupportRequests(
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        long userId = SessionAuth.requireUserId(sessionStore, token);
        Long teamId = teamController.getTeamIdOfUser(userId);
        if (teamId == null) {
            throw new IllegalArgumentException("Non hai un team");
        }

        long hackathonId = teamRegistrationController.getMyRegistration(userId)
                .orElseThrow(() -> new IllegalArgumentException("Il tuo team non e registrato a nessun hackathon."))
                .getHackathonId();

        return supportRequestRepository.findByHackathonId(hackathonId).stream()
                .filter(supportRequest -> supportRequest.getTeamId() == teamId)
                .sorted((a, b) -> Long.compare(a.getRequestId(), b.getRequestId()))
                .map(SupportRequestDto::from)
                .toList();
    }

    // Elenca le proposte call disponibili.
    @GetMapping("/calls/proposals")
    public List<CallProposalDto> listProposals(
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        long userId = SessionAuth.requireUserId(sessionStore, token);
        return supportController.listAvailableCallProposalsForCurrentUser(userId).stream()
                .map(CallProposalDto::from)
                .toList();
    }

    // Prenota una proposta call disponibile.
    @PostMapping("/calls/proposals/{proposalId}/book")
    public ResponseEntity<CallBookingDto> bookProposal(
            @PathVariable long proposalId,
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        if (proposalId <= 0) {
            throw new IllegalArgumentException("Call proposal non valida");
        }

        long userId = SessionAuth.requireUserId(sessionStore, token);
        CallProposal proposal = callProposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("Call proposal non trovata"));

        CallBooking booking = supportController.bookCall(userId, proposalId);
        CallBookingDto response = new CallBookingDto(
                booking.getCallId(),
                booking.getProposalId(),
                proposal.getProposedStart(),
                proposal.getProposedEnd(),
                booking.getMeetingLink()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Accetta solo le azioni consentite sugli inviti.
    private String normalizeAction(String action) {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("Action non valida");
        }
        String normalized = action.trim().toUpperCase(Locale.ROOT);
        if (!"ACCEPT".equals(normalized) && !"DECLINE".equals(normalized)) {
            throw new IllegalArgumentException("Action non valida");
        }
        return normalized;
    }

    // Recupera l'id team e fallisce se l'utente non e' in un team.
    private long requireTeamId(long userId) {
        Long teamId = teamController.getTeamIdOfUser(userId);
        if (teamId == null) {
            throw new IllegalArgumentException("Non hai un team");
        }
        return teamId;
    }

    // Valida e pulisce il contenuto della submission.
    private String normalizeContent(SubmitRequest request) {
        String content = request == null ? null : request.content();
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Contenuto submission non valido");
        }
        return content.trim();
    }

    private record InviteUserRequest(String invitedUsername) {
    }

    @GetMapping("/team/invitations/sent")
    public List<Invitation> sentInvitations(
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        return teamController.viewSentInvites(SessionAuth.requireUserId(sessionStore, token));
    }

    @PostMapping("/teams/{teamId}/invitations/batch")
    public List<Invitation> inviteUsers(@PathVariable long teamId,
            @RequestBody InviteUsersRequest request,
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        long userId = SessionAuth.requireUserId(sessionStore, token);
        if (request == null) throw new IllegalArgumentException("Destinatari obbligatori");
        return teamController.inviteUsers(userId, teamId, request.userIds());
    }

    @PostMapping("/invitations/{invitationId}/cancel")
    public Map<String, String> cancelInvitation(@PathVariable long invitationId,
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        teamController.cancelInvitation(SessionAuth.requireUserId(sessionStore, token), invitationId);
        return Map.of("status", "cancelled");
    }

    @PostMapping("/team/leave")
    public Map<String, String> leaveTeam(
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        teamController.leaveTeam(SessionAuth.requireUserId(sessionStore, token));
        return Map.of("status", "left");
    }

    private record InviteUsersRequest(List<Long> userIds) { }

    private record ManageInvitationRequest(String action) {
    }

    private record RegisterTeamRequest(long hackathonId) {
    }

    private record SubmitRequest(String content) {
    }

    private record CreateSupportRequest(String message) {
    }

    private record InvitationDto(long invitationId, long teamId, String teamName, String status) {
    }

    private record TeamDto(long teamId, String teamName) {
        private static TeamDto from(Team team) {
            return new TeamDto(team.getTeamId(), team.getTeamName());
        }
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
            java.math.BigDecimal prizeAmount,
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

    private record TeamRegistrationDto(
            long registrationId,
            long teamId,
            long hackathonId,
            boolean expelled,
            LocalDateTime registeredAt
    ) {
        private static TeamRegistrationDto from(TeamRegistration registration) {
            return new TeamRegistrationDto(
                    registration.getRegistrationId(),
                    registration.getTeamId(),
                    registration.getHackathonId(),
                    registration.isExpelled(),
                    registration.getRegisteredAt()
            );
        }
    }

    private record SubmissionDto(
            long submissionId,
            long registrationId,
            String content,
            LocalDateTime submittedAt,
            LocalDateTime updatedAt
    ) {
        private static SubmissionDto from(Submission submission) {
            return new SubmissionDto(
                    submission.getSubmissionId(),
                    submission.getRegistrationId(),
                    submission.getContent(),
                    submission.getSubmittedAt(),
                    submission.getLastUpdatedAt()
            );
        }
    }

    private record SupportRequestDto(
            long requestId,
            long hackathonId,
            long teamId,
            String message,
            LocalDateTime createdAt
    ) {
        private static SupportRequestDto from(SupportRequest supportRequest) {
            return new SupportRequestDto(
                    supportRequest.getRequestId(),
                    supportRequest.getHackathonId(),
                    supportRequest.getTeamId(),
                    (supportRequest.isCancelled() ? "[ANNULLATA] " : "[APERTA] ") + supportRequest.getMessage(),
                    supportRequest.getCreatedAt()
            );
        }
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

        private static CallProposalDto from(SupportController.CallProposalSummary summary) {
            return new CallProposalDto(
                    summary.proposalId(),
                    summary.requestId(),
                    summary.proposedStart(),
                    summary.proposedEnd(),
                    summary.booked()
            );
        }
    }

    private record CallBookingDto(
            long callId,
            long proposalId,
            LocalDateTime start,
            LocalDateTime end,
            String meetingLink
    ) {
    }
}
