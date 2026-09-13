package it.unicam.hackhub.controller;

import it.unicam.hackhub.external.CalendarSystem;
import it.unicam.hackhub.model.CallBooking;
import it.unicam.hackhub.model.CallProposal;
import it.unicam.hackhub.model.Hackathon;
import it.unicam.hackhub.model.StaffAssignment;
import it.unicam.hackhub.model.SupportRequest;
import it.unicam.hackhub.model.TeamRegistration;
import it.unicam.hackhub.model.User;
import it.unicam.hackhub.model.enums.HackathonStatus;
import it.unicam.hackhub.model.enums.StaffRole;
import it.unicam.hackhub.repository.CallProposalRepository;
import it.unicam.hackhub.repository.HackathonRepository;
import it.unicam.hackhub.repository.StaffAssignmentRepository;
import it.unicam.hackhub.repository.SupportRequestRepository;
import it.unicam.hackhub.repository.TeamRegistrationRepository;
import it.unicam.hackhub.repository.UserRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SupportController {
    private final SupportRequestRepository supportRequestRepository;
    private final CallProposalRepository callProposalRepository;
    private final UserRepository userRepository;
    private final TeamRegistrationRepository teamRegistrationRepository;
    private final HackathonRepository hackathonRepository;
    private final StaffAssignmentRepository staffAssignmentRepository;
    private final CalendarSystem calendarSystem;

    public SupportController(SupportRequestRepository supportRequestRepository,
                             CallProposalRepository callProposalRepository,
                             UserRepository userRepository,
                             TeamRegistrationRepository teamRegistrationRepository,
                             HackathonRepository hackathonRepository,
                             StaffAssignmentRepository staffAssignmentRepository,
                             CalendarSystem calendarSystem) {
        this.supportRequestRepository = supportRequestRepository;
        this.callProposalRepository = callProposalRepository;
        this.userRepository = userRepository;
        this.teamRegistrationRepository = teamRegistrationRepository;
        this.hackathonRepository = hackathonRepository;
        this.staffAssignmentRepository = staffAssignmentRepository;
        this.calendarSystem = calendarSystem;
    }

    // Crea una richiesta supporto usando il contesto team/hackathon dell'utente.
    public SupportRequest createSupportRequestForCurrentUser(long currentUserId, String message) {
        UserHackathonContext context = resolveUserHackathonContext(currentUserId);
        return createSupportRequest(currentUserId, context.hackathonId(), message);
    }

    // Elenca le proposte call ancora libere per il team corrente.
    public List<CallProposalSummary> listAvailableCallProposalsForCurrentUser(long currentUserId) {
        UserHackathonContext context = resolveUserHackathonContext(currentUserId);
        List<CallProposalSummary> summaries = new ArrayList<>();

        List<SupportRequest> teamRequests = supportRequestRepository.findByHackathonId(context.hackathonId()).stream()
                .filter(request -> request.getTeamId() == context.teamId() && !request.isCancelled())
                .toList();

        for (SupportRequest request : teamRequests) {
            for (CallProposal proposal : callProposalRepository.findBySupportRequestId(request.getRequestId())) {
                if (proposal.isBooked() || !proposal.getProposedStart().isAfter(LocalDateTime.now())) {
                    continue;
                }
                summaries.add(new CallProposalSummary(
                        proposal.getProposalId(),
                        proposal.getRequestId(),
                        proposal.getProposedStart(),
                        proposal.getProposedEnd(),
                        proposal.isBooked()
                ));
            }
        }

        return summaries;
    }

    // Restituisce gli hackathon assegnati al mentor corrente.
    public List<MentorHackathonView> listMentorAssignedHackathons(long currentStaffId) {
        return staffAssignmentRepository.findByStaffId(currentStaffId).stream()
                .filter(assignment -> assignment.getRole() == StaffRole.MENTOR)
                .map(StaffAssignment::getHackathonId)
                .distinct()
                .sorted()
                .map(hackathonId -> hackathonRepository.findById(hackathonId)
                        .map(hackathon -> new MentorHackathonView(
                                hackathonId,
                                safe(hackathon.getHackathonName()),
                                hackathon.getStatus() == null ? "-" : hackathon.getStatus().name(),
                                safe(hackathon.getLocation())
                        ))
                        .orElse(null))
                .filter(view -> view != null)
                .toList();
    }

    // Elenca le richieste supporto visibili al mentor in quell'hackathon.
    public List<SupportRequestView> listSupportRequestsForMentor(long currentStaffId, long hackathonId) {
        hackathonRepository.findById(hackathonId).orElseThrow(() -> new IllegalArgumentException("Hackathon non trovato"));
        boolean isMentorAssigned = staffAssignmentRepository.findByHackathonId(hackathonId).stream()
                .anyMatch(assignment -> assignment.getRole() == StaffRole.MENTOR
                        && assignment.getStaffId() == currentStaffId);
        if (!isMentorAssigned) {
            throw new IllegalArgumentException("Not authorized: staff not assigned as mentor");
        }

        return supportRequestRepository.findByHackathonId(hackathonId).stream()
                .sorted(Comparator.comparingLong(SupportRequest::getRequestId))
                .map(request -> new SupportRequestView(
                        request.getRequestId(),
                        request.getTeamId(),
                        request.getCreatedAt(),
                        (request.isCancelled() ? "[ANNULLATA] " : "[APERTA] ") + request.getMessage()
                ))
                .toList();
    }

    // Crea una richiesta supporto solo per team attivi e in RUNNING.
    public SupportRequest createSupportRequest(long currentUserId, long hackathonId, String message) {
        User user = userRepository.findById(currentUserId).filter(User::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));
        Long teamId = user.getTeamId();
        if (teamId == null) {
            throw new IllegalArgumentException("Non hai un team");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Messaggio non valido");
        }

        TeamRegistration registration = teamRegistrationRepository
                .findByTeamIdAndHackathonId(teamId, hackathonId)
                .orElseThrow(() -> new IllegalArgumentException("Team non registrato a questo hackathon"));
        // Team espulso: niente nuove richieste.
        if (registration.isExpelled()) {
            throw new IllegalStateException("Team expelled");
        }

        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new IllegalArgumentException("Hackathon non trovato"));
        // Le richieste supporto sono aperte solo durante RUNNING.
        if (hackathon.getStatus() != HackathonStatus.RUNNING) {
            throw new IllegalStateException("Richieste supporto consentite solo in stato RUNNING");
        }

        SupportRequest supportRequest = new SupportRequest(
                0L,
                teamId,
                hackathonId,
                message,
                LocalDateTime.now()
        );
        return supportRequestRepository.save(supportRequest);
    }

    // Il mentor propone una finestra call su una richiesta specifica.
    @org.springframework.transaction.annotation.Transactional
    public synchronized CallProposal createCallProposal(long currentStaffId,
                                           long supportRequestId,
                                           LocalDateTime proposedStart,
                                           LocalDateTime proposedEnd) {
        if (proposedStart == null || proposedEnd == null || !proposedStart.isBefore(proposedEnd)) {
            throw new IllegalArgumentException("Intervallo proposta non valido");
        }

        SupportRequest supportRequest = supportRequestRepository.findById(supportRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Support request non trovata"));
        long hackathonId = supportRequest.getHackathonId();

        // Solo mentor assegnati a quell'hackathon possono proporre slot.
        boolean isMentorAssigned = false;
        for (StaffAssignment assignment : staffAssignmentRepository.findByHackathonId(hackathonId)) {
            if (assignment.getRole() == StaffRole.MENTOR && assignment.getStaffId() == currentStaffId) {
                isMentorAssigned = true;
                break;
            }
        }
        if (!isMentorAssigned) {
            throw new IllegalArgumentException("Not authorized: only mentor can create call proposal");
        }

        Hackathon event = hackathonRepository.findById(hackathonId).orElseThrow(() -> new IllegalArgumentException("Hackathon non trovato"));
        TeamRegistration registration = teamRegistrationRepository.findByTeamIdAndHackathonId(supportRequest.getTeamId(), hackathonId)
                .orElseThrow(() -> new IllegalArgumentException("Team non iscritto"));
        if (supportRequest.isCancelled() || registration.isExpelled() || event.getStatus() != HackathonStatus.RUNNING
                || !proposedStart.isAfter(LocalDateTime.now()) || proposedStart.isBefore(event.getStartDate())
                || proposedEnd.isAfter(event.getEndDate())) throw new IllegalStateException("Richiesta o intervallo non disponibile");

        CallProposal proposal = new CallProposal(
                0L,
                supportRequestId,
                currentStaffId,
                proposedStart,
                proposedEnd,
                false
        );
        return callProposalRepository.save(proposal);
    }

    // Restituisce tutte le proposte legate a una richiesta supporto.
    public List<CallProposal> listCallProposals(long supportRequestId) {
        return callProposalRepository.findBySupportRequestId(supportRequestId);
    }

    // Prenota una proposta call valida per il team dell'utente.
    @org.springframework.transaction.annotation.Transactional
    public synchronized CallBooking bookCall(long currentUserId, long proposalId) {
        CallProposal proposal = callProposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("Call proposal non trovata"));
        if (proposal.isBooked()) {
            throw new IllegalStateException("Call proposal gia prenotata");
        }

        SupportRequest supportRequest = supportRequestRepository.findById(proposal.getRequestId())
                .orElseThrow(() -> new IllegalArgumentException("Support request non trovata"));

        User user = userRepository.findById(currentUserId).filter(User::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));
        Long teamId = user.getTeamId();
        if (teamId == null) {
            throw new IllegalArgumentException("Non hai un team");
        }
        if (teamId != supportRequest.getTeamId()) {
            throw new IllegalArgumentException("Non autorizzato: richiesta di un altro team");
        }

        TeamRegistration registration = teamRegistrationRepository
                .findByTeamIdAndHackathonId(teamId, supportRequest.getHackathonId())
                .orElseThrow(() -> new IllegalArgumentException("Team non registrato a questo hackathon"));
        // Team espulso: niente prenotazioni.
        if (registration.isExpelled()) {
            throw new IllegalStateException("Team expelled");
        }

        Hackathon event = hackathonRepository.findById(supportRequest.getHackathonId()).orElseThrow();
        if (supportRequest.isCancelled() || event.getStatus() != HackathonStatus.RUNNING
                || !proposal.getProposedStart().isAfter(LocalDateTime.now())
                || proposal.getProposedStart().isBefore(event.getStartDate()) || proposal.getProposedEnd().isAfter(event.getEndDate()))
            throw new IllegalStateException("Proposta non più prenotabile");

        // Se il calendario non genera il link, fermiamo tutto qui.
        String meetingLink = calendarSystem.createMeetingLink(
                proposal.getProposedStart(),
                proposal.getProposedEnd()
        );
        if (meetingLink == null || meetingLink.isBlank()) {
            String err = calendarSystem.getLastErrorMessage();
            throw new IllegalStateException(err != null ? err : "Impossibile creare il meeting");
        }

        CallBooking booking = new CallBooking(
                0L,
                proposalId,
                meetingLink,
                LocalDateTime.now()
        );
        CallBooking savedBooking = callProposalRepository.saveBooking(booking);

        // Segno la proposta occupata solo dopo aver salvato la prenotazione.
        proposal.setBooked(true);
        callProposalRepository.save(proposal);

        return savedBooking;
    }

    @org.springframework.transaction.annotation.Transactional
    public synchronized void cancelSupportRequest(long currentUserId, long requestId) {
        User user = userRepository.findById(currentUserId).filter(User::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Utente non disponibile"));
        SupportRequest request = supportRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Richiesta non trovata"));
        if (user.getTeamId() == null || user.getTeamId() != request.getTeamId())
            throw new IllegalArgumentException("Richiesta di un altro team");
        if (request.isCancelled() || callProposalRepository.findBySupportRequestId(requestId).stream().anyMatch(CallProposal::isBooked))
            throw new IllegalStateException("Richiesta annullata o con call già prenotata");
        request.setCancelled(true);
        supportRequestRepository.save(request);
    }

    // Risolve una volta sola il contesto team/hackathon dell'utente.
    private UserHackathonContext resolveUserHackathonContext(long currentUserId) {
        User user = userRepository.findById(currentUserId).filter(User::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));
        Long teamId = user.getTeamId();
        if (teamId == null) {
            throw new IllegalArgumentException("Non hai un team");
        }

        TeamRegistration registration = teamRegistrationRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Il tuo team non e registrato a nessun hackathon."));
        return new UserHackathonContext(teamId, registration.getHackathonId());
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }

    public record CallProposalSummary(long proposalId,
                                      long requestId,
                                      LocalDateTime proposedStart,
                                      LocalDateTime proposedEnd,
                                      boolean booked) {
    }

    public record MentorHackathonView(long hackathonId,
                                      String name,
                                      String status,
                                      String location) {
    }

    public record SupportRequestView(long requestId,
                                     long teamId,
                                     LocalDateTime createdAt,
                                     String message) {
    }

    private record UserHackathonContext(long teamId, long hackathonId) {
    }
}
