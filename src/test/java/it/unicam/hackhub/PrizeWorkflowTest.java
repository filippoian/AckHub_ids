package it.unicam.hackhub;

import it.unicam.hackhub.controller.HackathonController;
import it.unicam.hackhub.external.PaymentSystem;
import it.unicam.hackhub.external.PaymentSystemStub;
import it.unicam.hackhub.model.*;
import it.unicam.hackhub.model.enums.*;
import it.unicam.hackhub.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class PrizeWorkflowTest {
    @Autowired HackathonController controller;
    @Autowired HackathonRepository events;
    @Autowired StaffAssignmentRepository assignments;
    @Autowired TeamRepository teams;
    @Autowired TeamRegistrationRepository registrations;
    @Autowired SubmissionRepository submissions;
    @Autowired EvaluationRepository evaluations;
    @MockitoBean PaymentSystem payment;
    long eventId;
    long teamId;
    String teamName;
    final BigDecimal prize = new BigDecimal("1000.00");

    @BeforeEach void fixture() {
        reset(payment);
        var event = new Hackathon();
        event.setStatus(HackathonStatus.REVIEW);
        event.setPrizeAmount(prize);
        eventId = events.save(event).getHackathonId();
        assignments.save(new StaffAssignment(0, 901, eventId, StaffRole.ORGANIZER));
        teamName = "Team-" + UUID.randomUUID();
        teamId = teams.save(new Team(0, teamName)).getTeamId();
        var reg = registrations.save(new TeamRegistration(0, teamId, eventId, LocalDateTime.now(), false));
        var sub = submissions.save(new Submission(0, reg.getRegistrationId(), "soluzione", LocalDateTime.now(), LocalDateTime.now()));
        evaluations.save(new Evaluation(0, sub.getSubmissionId(), 30, "giudizio", LocalDateTime.now()));
    }

    @Test void failedPaymentKeepsCommittedWinnerAndRetryClosesEventOnce() {
        controller.setWinner(901, eventId, teamId);
        verifyNoInteractions(payment);
        assertEquals(teamId, events.findById(eventId).orElseThrow().getWinnerTeamId());
        when(payment.payPrize(eq(eventId), any(BigDecimal.class), eq(teamName))).thenReturn(false, true);
        when(payment.getLastReceiptId()).thenReturn("receipt-123");
        assertTrue(controller.payPrize(901, eventId).contains("fallito"));
        var failed = events.findById(eventId).orElseThrow();
        assertEquals(teamId, failed.getWinnerTeamId());
        assertFalse(failed.isPrizePaid()); assertEquals(HackathonStatus.REVIEW, failed.getStatus());
        assertThrows(IllegalStateException.class, () -> controller.setWinner(901, eventId, teamId + 999));
        controller.setWinner(901, eventId, teamId);
        assertTrue(controller.payPrize(901, eventId).contains("completato"));
        var paid = events.findById(eventId).orElseThrow();
        assertTrue(paid.isPrizePaid()); assertEquals(HackathonStatus.CLOSED, paid.getStatus());
        assertEquals("receipt-123", paid.getPrizeReceiptId());
        assertTrue(controller.payPrize(901, eventId).contains("gia erogato"));
        verify(payment, times(2)).payPrize(eq(eventId), any(BigDecimal.class), eq(teamName));
    }

    @Test void serviceExceptionDoesNotUndoPreviouslyCommittedProclamation() {
        controller.setWinner(901, eventId, teamId);
        when(payment.payPrize(eq(eventId), any(BigDecimal.class), eq(teamName))).thenThrow(new IllegalStateException("Servizio indisponibile"));
        assertThrows(IllegalStateException.class, () -> controller.payPrize(901, eventId));
        var loaded = events.findById(eventId).orElseThrow();
        assertEquals(teamId, loaded.getWinnerTeamId()); assertFalse(loaded.isPrizePaid());
        assertEquals(HackathonStatus.REVIEW, loaded.getStatus());
    }

    @Test void prematureClosingAndUnauthorizedPaymentAreRejected() {
        assertThrows(IllegalStateException.class, () -> controller.advanceStatus(901, eventId));
        assertThrows(IllegalStateException.class, () -> controller.payPrize(901, eventId));
        controller.setWinner(901, eventId, teamId);
        assertThrows(IllegalStateException.class, () -> controller.advanceStatus(901, eventId));
        assertThrows(IllegalArgumentException.class, () -> controller.payPrize(902, eventId));
        verifyNoInteractions(payment);
    }

    @Autowired ViolationReportRepository reports;

    @Test void teamsWithoutSubmissionAndExpelledTeamsDoNotBlockWinner() {
        long absent = teams.save(new Team(0, "absent-" + UUID.randomUUID())).getTeamId();
        registrations.save(new TeamRegistration(0, absent, eventId, LocalDateTime.now(), false));
        reports.save(new ViolationReport(0, eventId, absent, 903, "warning without submission", LocalDateTime.now(), "TEAM_WARNED"));
        long expelled = teams.save(new Team(0, "expelled-" + UUID.randomUUID())).getTeamId();
        var expelledReg = registrations.save(new TeamRegistration(0, expelled, eventId, LocalDateTime.now(), true));
        submissions.save(new Submission(0, expelledReg.getRegistrationId(), "ungraded", LocalDateTime.now(), LocalDateTime.now()));
        assertThrows(IllegalStateException.class, () -> controller.setWinner(901, eventId, absent));
        assertThrows(IllegalStateException.class, () -> controller.setWinner(901, eventId, expelled));
        assertEquals(teamId, controller.setWinner(901, eventId, teamId).getWinnerTeamId());
    }

    @Test void anotherEligibleSubmissionMustBeEvaluatedBeforeProclamation() {
        long other = teams.save(new Team(0, "other-" + UUID.randomUUID())).getTeamId();
        var reg = registrations.save(new TeamRegistration(0, other, eventId, LocalDateTime.now(), false));
        var submission = submissions.save(new Submission(0, reg.getRegistrationId(), "ungraded", LocalDateTime.now(), LocalDateTime.now()));
        assertThrows(IllegalStateException.class, () -> controller.setWinner(901, eventId, teamId));
        assertNull(events.findById(eventId).orElseThrow().getWinnerTeamId());
        evaluations.save(new Evaluation(0, submission.getSubmissionId(), 20, "graded", LocalDateTime.now()));
        controller.setWinner(901, eventId, teamId);
    }

    @Test void zeroPrizeClosesWithoutExternalPaymentAndIsIdempotent() {
        var event = events.findById(eventId).orElseThrow(); event.setPrizeAmount(BigDecimal.ZERO); events.save(event);
        controller.setWinner(901, eventId, teamId);
        controller.payPrize(901, eventId);
        var closed = events.findById(eventId).orElseThrow();
        assertTrue(closed.isPrizePaid()); assertEquals(HackathonStatus.CLOSED, closed.getStatus());
        assertEquals("ZERO-" + eventId, closed.getPrizeReceiptId());
        assertTrue(controller.payPrize(901, eventId).contains("gia erogato"));
        verifyNoInteractions(payment);
    }

    @Test void stubReusesReceiptForSameHackathonAndSeparatesDifferentEvents() {
        var stub = new PaymentSystemStub();
        assertTrue(stub.payPrize(1, prize, teamName));
        String receipt = stub.getLastReceiptId();
        assertTrue(stub.payPrize(1, prize, teamName));
        assertEquals(receipt, stub.getLastReceiptId());
        assertTrue(stub.payPrize(2, prize, teamName));
        assertNotEquals(receipt, stub.getLastReceiptId());
    }
}
