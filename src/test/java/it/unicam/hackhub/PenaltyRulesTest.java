package it.unicam.hackhub;

import it.unicam.hackhub.controller.*;
import it.unicam.hackhub.external.PaymentSystem;
import it.unicam.hackhub.model.*;
import it.unicam.hackhub.model.enums.*;
import it.unicam.hackhub.repository.*;
import it.unicam.hackhub.repository.inmemory.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PenaltyRulesTest {
    StaffAssignmentRepository staff = mock(StaffAssignmentRepository.class);
    HackathonRepository events = mock(HackathonRepository.class);
    SubmissionRepository submissions = mock(SubmissionRepository.class);
    TeamRegistrationRepository registrations = mock(TeamRegistrationRepository.class);
    InMemoryEvaluationRepository evaluations = new InMemoryEvaluationRepository();
    InMemoryViolationReportRepository reports = new InMemoryViolationReportRepository();
    EvaluationController controller;
    Hackathon event = new Hackathon();
    TeamRegistration registration = new TeamRegistration(2, 3, 1, LocalDateTime.now(), false);
    Submission submission = mock(Submission.class);

    @BeforeEach void prepare() {
        event.setHackathonId(1); event.setStatus(HackathonStatus.REVIEW);
        StaffAssignment assignment = mock(StaffAssignment.class);
        when(assignment.getStaffId()).thenReturn(7L);
        when(assignment.getRole()).thenReturn(StaffRole.JUDGE);
        when(staff.findByHackathonIdAndRole(1, StaffRole.JUDGE)).thenReturn(List.of(assignment));
        when(events.findById(1)).thenReturn(Optional.of(event));
        when(submission.getSubmissionId()).thenReturn(4L);
        when(submission.getRegistrationId()).thenReturn(2L);
        when(submissions.findById(4)).thenReturn(Optional.of(submission));
        when(submissions.findByRegistrationId(2)).thenReturn(Optional.of(submission));
        when(registrations.findById(2)).thenReturn(Optional.of(registration));
        when(registrations.findByTeamIdAndHackathonId(3, 1)).thenReturn(Optional.of(registration));
        controller = new EvaluationController(staff, events, evaluations, submissions, registrations, reports);
    }

    ViolationReport warning() {
        return reports.save(new ViolationReport(0, 1, 3, 9, "violazione", LocalDateTime.now(), "TEAM_WARNED"));
    }

    @Test void acceptsThirtyRejectsThirtyOneAndRequiresComment() {
        assertEquals(30, controller.evaluateSubmission(7, 4, 30, "ottimo").getScore());
        assertThrows(IllegalArgumentException.class, () -> controller.evaluateSubmission(7, 4, 31, "x"));
        assertThrows(IllegalArgumentException.class, () -> controller.evaluateSubmission(7, 4, 20, " "));
    }

    @Test void sumsWarningsAndReplacesPenaltyWithoutDoubleCounting() {
        var first = warning(); var second = warning();
        controller.evaluateWarning(7, first.getReportId(), 5);
        controller.evaluateWarning(7, second.getReportId(), 8);
        var evaluation = controller.evaluateSubmission(7, 4, 30, "giudizio");
        assertEquals(17, evaluation.getScore()); assertEquals(13, evaluation.getPenalty());
        controller.evaluateWarning(7, first.getReportId(), 2);
        assertEquals(20, evaluation.getScore()); assertEquals(10, evaluation.getPenalty());
        controller.evaluateSubmission(7, 4, 25, "modifica");
        assertEquals(15, evaluation.getScore());
        controller.evaluateWarning(7, second.getReportId(), 30);
        assertEquals(0, evaluation.getScore()); assertEquals(25, evaluation.getBaseScore());
    }

    @Test void zeroIsAnEvaluatedPenaltyAndPendingWarningBlocksSubmission() {
        var warning = warning();
        assertThrows(IllegalStateException.class, () -> controller.evaluateSubmission(7, 4, 30, "x"));
        controller.evaluateWarning(7, warning.getReportId(), 0);
        assertTrue(warning.isPenaltyEvaluated());
        assertEquals(30, controller.evaluateSubmission(7, 4, 30, "x").getScore());
    }

    @Test void rejectsUnauthorizedWrongPhaseAndInvalidPenalty() {
        var warning = warning();
        assertThrows(IllegalArgumentException.class, () -> controller.evaluateWarning(8, warning.getReportId(), 2));
        assertThrows(IllegalArgumentException.class, () -> controller.evaluateWarning(7, warning.getReportId(), -1));
        assertThrows(IllegalArgumentException.class, () -> controller.evaluateWarning(7, warning.getReportId(), 31));
        event.setStatus(HackathonStatus.RUNNING);
        assertThrows(IllegalStateException.class, () -> controller.evaluateWarning(7, warning.getReportId(), 2));
        assertFalse(warning.isPenaltyEvaluated());
    }

    @Test void freezesEvaluationsAfterProclamation() {
        var warning = warning(); event.setWinnerTeamId(3L);
        assertThrows(IllegalStateException.class, () -> controller.evaluateWarning(7, warning.getReportId(), 2));
        assertThrows(IllegalStateException.class, () -> controller.evaluateSubmission(7, 4, 30, "x"));
    }

    @Test void pendingWarningBlocksWinnerBeforePayment() {
        evaluations.save(new Evaluation(0, 4, 20, "giudizio", LocalDateTime.now()));
        warning();
        StaffAssignment organizer = mock(StaffAssignment.class);
        when(organizer.getStaffId()).thenReturn(8L);
        when(organizer.getRole()).thenReturn(StaffRole.ORGANIZER);
        when(staff.findByHackathonIdAndRole(1, StaffRole.ORGANIZER)).thenReturn(List.of(organizer));
        PaymentSystem payment = mock(PaymentSystem.class);
        HackathonController hackathons = new HackathonController(events, staff, mock(StaffMemberRepository.class),
                registrations, submissions, evaluations, mock(TeamRepository.class), payment, reports, mock(UserRepository.class));
        var ex = assertThrows(IllegalStateException.class, () -> hackathons.setWinner(8, 1, 3));
        assertTrue(ex.getMessage().contains("ammonizioni"));
        verifyNoInteractions(payment); assertNull(event.getWinnerTeamId());
    }
}
