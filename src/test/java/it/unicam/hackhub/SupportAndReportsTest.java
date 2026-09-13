package it.unicam.hackhub;

import it.unicam.hackhub.controller.*;
import it.unicam.hackhub.external.CalendarSystem;
import it.unicam.hackhub.model.*;
import it.unicam.hackhub.model.enums.*;
import it.unicam.hackhub.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class SupportAndReportsTest {
    @Autowired SupportController support;
    @Autowired ViolationReportController discipline;
    @Autowired HackathonRepository events;
    @Autowired TeamRepository teams;
    @Autowired UserRepository users;
    @Autowired TeamRegistrationRepository registrations;
    @Autowired StaffAssignmentRepository assignments;
    @Autowired SupportRequestRepository requests;
    @Autowired CallProposalRepository proposals;
    @Autowired ViolationReportRepository reports;
    @MockitoBean CalendarSystem calendar;
    long eventId, teamId, userId, registrationId;
    LocalDateTime start, end;

    @BeforeEach void fixture() {
        reset(calendar);
        var event = new Hackathon(); event.setStatus(HackathonStatus.RUNNING);
        event.setStartDate(LocalDateTime.now().minusHours(1)); event.setEndDate(LocalDateTime.now().plusHours(3));
        eventId = events.save(event).getHackathonId();
        teamId = teams.save(new Team(0, "t-"+UUID.randomUUID())).getTeamId();
        userId = users.save(new User(0, "u-"+UUID.randomUUID(), "hash", teamId)).getUserId();
        registrationId = registrations.save(new TeamRegistration(0, teamId, eventId, LocalDateTime.now(), false)).getRegistrationId();
        assignments.save(new StaffAssignment(0, 5501, eventId, StaffRole.MENTOR));
        assignments.save(new StaffAssignment(0, 5502, eventId, StaffRole.ORGANIZER));
        assignments.save(new StaffAssignment(0, 5503, eventId, StaffRole.JUDGE));
        start = LocalDateTime.now().plusHours(1); end = start.plusMinutes(30);
    }

    @Test void cancellationKeepsHistoryAndPreventsBothProposalAndBooking() {
        var request = support.createSupportRequest(userId, eventId, "help");
        var proposal = support.createCallProposal(5501, request.getRequestId(), start, end);
        assertThrows(IllegalArgumentException.class, () -> support.cancelSupportRequest(999, request.getRequestId()));
        support.cancelSupportRequest(userId, request.getRequestId());
        assertTrue(requests.findById(request.getRequestId()).orElseThrow().isCancelled());
        assertTrue(support.listAvailableCallProposalsForCurrentUser(userId).isEmpty());
        assertTrue(support.listSupportRequestsForMentor(5501, eventId).get(0).message().contains("ANNULLATA"));
        assertThrows(IllegalStateException.class, () -> support.bookCall(userId, proposal.getProposalId()));
        assertThrows(IllegalStateException.class, () -> support.createCallProposal(5501, request.getRequestId(), start, end));
        verifyNoInteractions(calendar);
    }

    @Test void calendarFailureIsRetryableAndSuccessfulBookingCannotBeCancelledOrDuplicated() {
        var request = support.createSupportRequest(userId, eventId, "help");
        var proposal = support.createCallProposal(5501, request.getRequestId(), start, end);
        when(calendar.createMeetingLink(any(), any())).thenReturn(null, "https://calendar.stub/meeting/test");
        assertThrows(IllegalStateException.class, () -> support.bookCall(userId, proposal.getProposalId()));
        assertFalse(proposals.findById(proposal.getProposalId()).orElseThrow().isBooked());
        assertTrue(proposals.findBookingByProposalId(proposal.getProposalId()).isEmpty());
        var booking = support.bookCall(userId, proposal.getProposalId());
        assertEquals("https://calendar.stub/meeting/test", booking.getMeetingLink());
        assertTrue(proposals.findById(proposal.getProposalId()).orElseThrow().isBooked());
        assertThrows(IllegalStateException.class, () -> support.cancelSupportRequest(userId, request.getRequestId()));
        assertThrows(IllegalStateException.class, () -> support.bookCall(userId, proposal.getProposalId()));
        verify(calendar, times(2)).createMeetingLink(any(), any());
    }

    @Test void proposalDatesAndExpelledTeamsAreRejected() {
        var request = support.createSupportRequest(userId, eventId, "help");
        assertThrows(IllegalStateException.class, () -> support.createCallProposal(5501, request.getRequestId(), start.minusDays(1), end.minusDays(1)));
        assertThrows(IllegalStateException.class, () -> support.createCallProposal(5501, request.getRequestId(), start, end.plusDays(1)));
        var reg = registrations.findById(registrationId).orElseThrow(); reg.setExpelled(true); registrations.save(reg);
        assertThrows(IllegalStateException.class, () -> support.createCallProposal(5501, request.getRequestId(), start, end));
        assertThrows(IllegalStateException.class, () -> discipline.createReport(5501, eventId, teamId, "again"));
        verifyNoInteractions(calendar);
    }

    @Test void authorMayEditAndWithdrawOnlyPendingReportAndJudgeHasSeparateLists() {
        var report = discipline.createReport(5501, eventId, teamId, "first"); long id = report.getReportId();
        assertThrows(IllegalArgumentException.class, () -> discipline.updateReport(5502, id, "wrong"));
        assertThrows(IllegalArgumentException.class, () -> discipline.updateReport(5501, id, " "));
        assertEquals("corrected", discipline.updateReport(5501, id, "corrected").getDescription());
        discipline.withdrawReport(5501, id);
        assertEquals("WITHDRAWN", reports.findById(id).orElseThrow().getDecision());
        assertTrue(reports.findPendingByHackathonId(eventId).isEmpty());
        assertThrows(IllegalStateException.class, () -> discipline.manageReport(5502, id, "WARN"));
        assertThrows(IllegalStateException.class, () -> discipline.updateReport(5501, id, "again"));
        var warning = discipline.createReport(5501, eventId, teamId, "warning");
        discipline.manageReport(5502, warning.getReportId(), "WARN");
        var expulsion = discipline.createReport(5501, eventId, teamId, "expulsion");
        discipline.manageReport(5502, expulsion.getReportId(), "EXPEL");
        assertEquals(1, discipline.listWarnings(5503, eventId).size());
        assertEquals(1, discipline.listExpulsions(5503, eventId).size());
        assertThrows(IllegalArgumentException.class, () -> discipline.listExpulsions(5501, eventId));
    }
}
