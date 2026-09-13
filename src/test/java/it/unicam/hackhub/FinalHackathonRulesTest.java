package it.unicam.hackhub;

import it.unicam.hackhub.controller.*;
import it.unicam.hackhub.model.*;
import it.unicam.hackhub.model.enums.*;
import it.unicam.hackhub.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class FinalHackathonRulesTest {
    @Autowired HackathonController controller;
    @Autowired HackathonRepository events;
    @Autowired TeamRepository teams;
    @Autowired UserRepository users;
    @Autowired TeamRegistrationRepository registrations;
    @Autowired StaffAssignmentRepository assignments;
    @Autowired StaffMemberRepository staff;
    @Autowired MentorManagementController mentors;
    @Autowired TeamController teamController;

    Hackathon event(HackathonStatus status) {
        LocalDateTime finish = LocalDateTime.now().plusDays(3);
        var e = Hackathon.builder().hackathonName("event-" + UUID.randomUUID()).regulation("rules")
                .registrationDeadline(LocalDateTime.now().plusDays(1)).startDate(LocalDateTime.now().plusDays(2))
                .endDate(finish).submissionDeadline(finish)
                .location("Camerino").maxTeamSize(4).prizeAmount(BigDecimal.ZERO).status(status).build();
        e = events.save(e); assignments.save(new StaffAssignment(0, 4401, e.getHackathonId(), StaffRole.ORGANIZER));
        return e;
    }

    @Test void updatesKeepExactTimesAmountsAndRejectCapacityAndDeadlineChangesAtomically() {
        var e = event(HackathonStatus.REGISTRATION); long id = e.getHackathonId();
        long team = teams.save(new Team(0, "t-"+UUID.randomUUID())).getTeamId();
        users.save(new User(0, "u-"+UUID.randomUUID(), "hash", team)); users.save(new User(0, "u-"+UUID.randomUUID(), "hash", team));
        registrations.save(new TeamRegistration(0, team, id, LocalDateTime.now(), false));
        LocalDateTime end = e.getEndDate();
        assertThrows(IllegalStateException.class, () -> controller.updateHackathon(4401, id, e.getHackathonName(), "changed", e.getRegistrationDeadline(), e.getStartDate(), end, end, "X", BigDecimal.ONE, 1));
        assertEquals("rules", events.findById(id).orElseThrow().getRegulation());
        assertThrows(IllegalArgumentException.class, () -> controller.updateHackathon(999, id, e.getHackathonName(), "changed", e.getRegistrationDeadline(), e.getStartDate(), end, end, "X", BigDecimal.ONE, 4));
        var updated = controller.updateHackathon(4401, id, e.getHackathonName(), "updated", e.getRegistrationDeadline(), e.getStartDate(), end, end, "Rome", new BigDecimal("123.45"), 3);
        assertEquals(new BigDecimal("123.45"), updated.getPrizeAmount()); assertEquals(end, updated.getEndDate());
        assertThrows(IllegalStateException.class, () -> controller.deleteHackathon(4401, id));
    }

    @Test void softDeletedEventsDisappearFromAllQueriesAndCannotBeUsed() {
        var e = event(HackathonStatus.REGISTRATION); long id = e.getHackathonId();
        controller.deleteHackathon(4401, id);
        assertTrue(events.findById(id).isEmpty()); assertTrue(events.findByName(e.getHackathonName()).isEmpty());
        assertTrue(controller.listHackathons().stream().noneMatch(h -> h.getHackathonId() == id));
        assertThrows(IllegalArgumentException.class, () -> controller.advanceStatus(4401, id));
    }

    @Test void phaseDatesAreEnforcedAndRosterIsFrozenInPersistence() {
        var e = event(HackathonStatus.REGISTRATION); long id = e.getHackathonId();
        long team = teams.save(new Team(0, "t-"+UUID.randomUUID())).getTeamId();
        long user = users.save(new User(0, "u-"+UUID.randomUUID(), "hash", team)).getUserId();
        long reg = registrations.save(new TeamRegistration(0, team, id, LocalDateTime.now(), false)).getRegistrationId();
        assertThrows(IllegalStateException.class, () -> controller.advanceStatus(4401, id));
        assertTrue(registrations.findById(reg).orElseThrow().getParticipantUserIds().isEmpty());
        e.setStartDate(LocalDateTime.now().minusHours(1)); events.save(e);
        assertEquals(HackathonStatus.RUNNING, controller.advanceStatus(4401, id).getStatus());
        assertEquals(List.of(user), registrations.findById(reg).orElseThrow().getParticipantUserIds());
        assertThrows(IllegalStateException.class, () -> controller.advanceStatus(4401, id));
        assertThrows(IllegalStateException.class, () -> teamController.leaveTeam(user));
        e = events.findById(id).orElseThrow(); e.setEndDate(LocalDateTime.now().minusMinutes(1)); events.save(e);
        assertEquals(HackathonStatus.REVIEW, controller.advanceStatus(4401, id).getStatus());
        assertThrows(IllegalStateException.class, () -> teamController.leaveTeam(user));
    }

    @Test void creationPreservesInstantsAndMoneyAndRejectsPartialStaffAssignments() {
        long organizer = staff.save(new StaffMember(0, "Organizer", "org-" + UUID.randomUUID(), "hash")).getStaffId();
        long judge = staff.save(new StaffMember(0, "Judge", "judge-" + UUID.randomUUID(), "hash")).getStaffId();
        long mentor = staff.save(new StaffMember(0, "Mentor", "mentor-" + UUID.randomUUID(), "hash")).getStaffId();
        var previous = event(HackathonStatus.CLOSED);
        assignments.save(new StaffAssignment(0, organizer, previous.getHackathonId(), StaffRole.ORGANIZER));
        String name = "created-" + UUID.randomUUID();
        var deadline = LocalDateTime.now().plusDays(1).withNano(0);
        var start = deadline.plusHours(2); var end = start.plusDays(1);
        assertThrows(IllegalArgumentException.class, () -> controller.createHackathon(organizer, name, "rules", deadline, start, end, end,
                "Camerino", new BigDecimal("123.45"), 4, judge, List.of(mentor, Long.MAX_VALUE)));
        assertTrue(events.findByName(name).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> controller.createHackathon(judge, name, "rules", deadline, start, end, end,
                "Camerino", BigDecimal.ZERO, 4, judge, List.of(mentor)));
        assertThrows(IllegalArgumentException.class, () -> controller.createHackathon(organizer, name, "rules", deadline, start, end, end.minusHours(1),
                "Camerino", BigDecimal.ZERO, 4, judge, List.of(mentor)));
        var result = controller.createHackathon(organizer, name, "rules", deadline, start, end, end,
                "Camerino", new BigDecimal("123.45"), 4, judge, List.of(mentor));
        var loaded = events.findById(result.getHackathonId()).orElseThrow();
        assertEquals(start, loaded.getStartDate()); assertEquals(end, loaded.getSubmissionDeadline());
        assertEquals(new BigDecimal("123.45"), loaded.getPrizeAmount());
        assertEquals(3, assignments.findByHackathonId(result.getHackathonId()).size());
        assertEquals(1, assignments.findByHackathonIdAndRole(result.getHackathonId(), StaffRole.JUDGE).size());
        long extra = staff.save(new StaffMember(0, "Extra", "extra-" + UUID.randomUUID(), "hash")).getStaffId();
        assertThrows(IllegalArgumentException.class, () -> mentors.addMentors(organizer, result.getHackathonId(), List.of(extra, Long.MAX_VALUE)));
        assertEquals(3, assignments.findByHackathonId(result.getHackathonId()).size());
    }

    @Test void closedHackathonRejectsAdditionalMentors() {
        var e = event(HackathonStatus.CLOSED);
        assertThrows(IllegalStateException.class, () -> mentors.addMentor(4401, e.getHackathonId(), 2));
    }
}
