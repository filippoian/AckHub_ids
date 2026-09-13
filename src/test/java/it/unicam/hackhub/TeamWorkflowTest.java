package it.unicam.hackhub;

import it.unicam.hackhub.controller.TeamController;
import it.unicam.hackhub.model.*;
import it.unicam.hackhub.model.enums.*;
import it.unicam.hackhub.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TeamWorkflowTest {
    @Autowired TeamController controller;
    @Autowired UserRepository users;
    @Autowired TeamRepository teams;
    @Autowired InvitationRepository invites;
    @Autowired HackathonRepository events;
    @Autowired TeamRegistrationRepository registrations;
    long member, first, second, outsider, teamId;
    User user() { return users.save(new User(0, "u-" + UUID.randomUUID(), "hash", null)); }
    @BeforeEach void setup() {
        member=user().getUserId(); first=user().getUserId(); second=user().getUserId(); outsider=user().getUserId();
        teamId=controller.createTeam(member, "t-"+UUID.randomUUID()).getTeamId();
    }
    Hackathon event(HackathonStatus status, int capacity) {
        Hackathon event = new Hackathon(); event.setStatus(status); event.setMaxTeamSize(capacity);
        event=events.save(event);
        registrations.save(new TeamRegistration(0, teamId, event.getHackathonId(), LocalDateTime.now(), false));
        return event;
    }
    @Test void bulkInvitationRequiresAcceptanceAndCancellationPreventsJoining() {
        var created=controller.inviteUsers(member,teamId,List.of(first,second));
        assertEquals(2,created.size()); assertNull(users.findById(first).orElseThrow().getTeamId());
        controller.cancelInvitation(member,created.get(0).getInvitationId());
        assertEquals(InvitationStatus.CANCELLED,invites.findById(created.get(0).getInvitationId()).orElseThrow().getStatus());
        assertFalse(controller.acceptInvitation(created.get(0).getInvitationId(),first));
        assertTrue(controller.acceptInvitation(created.get(1).getInvitationId(),second));
        assertEquals(teamId,users.findById(second).orElseThrow().getTeamId());
    }
    @Test void invalidRecipientDoesNotCreatePartialBatch() {
        assertThrows(IllegalArgumentException.class,()->controller.inviteUsers(member,teamId,List.of(first,Long.MAX_VALUE)));
        assertTrue(invites.findByTeamId(teamId).isEmpty());
    }
    @Test void outsiderCannotInviteOrRevoke() {
        assertThrows(IllegalArgumentException.class,()->controller.inviteUser(outsider,teamId,first));
        var invitation=controller.inviteUser(member,teamId,first);
        assertThrows(IllegalArgumentException.class,()->controller.cancelInvitation(outsider,invitation.getInvitationId()));
        assertEquals(InvitationStatus.PENDING,invites.findById(invitation.getInvitationId()).orElseThrow().getStatus());
    }
    @Test void runningAndReviewBlockJoiningAndLeavingButClosedAllowsThem() {
        var invitation=controller.inviteUser(member,teamId,first);
        var event=event(HackathonStatus.RUNNING,3);
        assertThrows(IllegalStateException.class,()->controller.acceptInvitation(invitation.getInvitationId(),first));
        assertThrows(IllegalStateException.class,()->controller.leaveTeam(member));
        event.setStatus(HackathonStatus.REVIEW); events.save(event);
        assertThrows(IllegalStateException.class,()->controller.inviteUser(member,teamId,second));
        assertThrows(IllegalStateException.class,()->controller.leaveTeam(member));
        event.setStatus(HackathonStatus.CLOSED); events.save(event);
        assertTrue(controller.acceptInvitation(invitation.getInvitationId(),first));
        controller.leaveTeam(member); assertNull(users.findById(member).orElseThrow().getTeamId());
    }
    @Test void capacityIsCheckedAgainWhenInvitationIsAccepted() {
        var firstInvite=controller.inviteUser(member,teamId,first);
        var secondInvite=controller.inviteUser(member,teamId,second);
        event(HackathonStatus.REGISTRATION,2);
        assertTrue(controller.acceptInvitation(firstInvite.getInvitationId(),first));
        assertThrows(IllegalStateException.class,()->controller.acceptInvitation(secondInvite.getInvitationId(),second));
        assertNull(users.findById(second).orElseThrow().getTeamId());
    }
    @Test void lastMemberCancelsPendingInvitesAndPreservesTeamHistory() {
        var invitation=controller.inviteUser(member,teamId,first);
        controller.leaveTeam(member);
        assertTrue(teams.findById(teamId).isPresent());
        assertEquals(InvitationStatus.CANCELLED,invites.findById(invitation.getInvitationId()).orElseThrow().getStatus());
    }
}
