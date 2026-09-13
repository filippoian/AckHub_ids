package it.unicam.hackhub;

import it.unicam.hackhub.controller.*;
import it.unicam.hackhub.cli.SessionContext;
import it.unicam.hackhub.model.*;
import it.unicam.hackhub.model.enums.*;
import it.unicam.hackhub.repository.*;
import it.unicam.hackhub.security.PasswordHasher;
import it.unicam.hackhub.web.session.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ProfileWorkflowTest {
    @Autowired AuthController auth;
    @Autowired TeamController teams;
    @Autowired UserRepository users;
    @Autowired HackathonRepository events;
    @Autowired TeamRegistrationRepository registrations;
    @Autowired InvitationRepository invitations;
    @Autowired InMemorySessionStore sessions;
    SessionContext cli = new SessionContext();

    RegistrationResult register() { return auth.registerUser("u-" + UUID.randomUUID(), "password"); }

    @Test void recoveryRotatesCodeAndInvalidatesAllUserSessionsButNotStaff() {
        var result = register(); long id = result.profile().userId();
        String first = sessions.create(new SessionPrincipal(SessionPrincipal.ProfileType.USER, id));
        String second = sessions.create(new SessionPrincipal(SessionPrincipal.ProfileType.USER, id));
        String staff = sessions.create(new SessionPrincipal(SessionPrincipal.ProfileType.STAFF, id));
        auth.registerSessionInvalidator(cli);
        cli.loginUser(id);
        var loaded = users.findById(id).orElseThrow();
        assertNotEquals(result.recoveryCode(), loaded.getRecoveryCodeHash());
        assertTrue(PasswordHasher.verifyPassword(result.recoveryCode(), loaded.getRecoveryCodeHash()));
        assertThrows(IllegalArgumentException.class, () -> auth.recoverCredentials(result.profile().userName(), "wrong", "new"));
        assertTrue(sessions.find(first).isPresent());
        var recovered = auth.recoverCredentials(result.profile().userName(), result.recoveryCode(), "newPassword");
        assertNotEquals(result.recoveryCode(), recovered.recoveryCode());
        assertTrue(sessions.find(first).isEmpty()); assertTrue(sessions.find(second).isEmpty());
        assertTrue(sessions.find(staff).isPresent()); assertFalse(cli.isUserLoggedIn());
        assertThrows(IllegalArgumentException.class, () -> auth.recoverCredentials(result.profile().userName(), result.recoveryCode(), "reuse"));
        assertTrue(PasswordHasher.verifyPassword("newPassword", users.findById(id).orElseThrow().getPasswordHash()));
        assertNotNull(auth.recoverCredentials(result.profile().userName(), recovered.recoveryCode(), "last"));
    }

    @Test void profileChangesAreValidatedBeforeSavingAndRequireNewLogin() {
        var a = register(); var b = register(); long id = a.profile().userId();
        var token = sessions.create(new SessionPrincipal(SessionPrincipal.ProfileType.USER, id));
        assertThrows(IllegalArgumentException.class, () -> auth.updateProfile(id, b.profile().userName(), null, "password", null));
        assertThrows(IllegalArgumentException.class, () -> auth.updateProfile(id, "renamed", "bad email", "password", "new"));
        assertThrows(IllegalArgumentException.class, () -> auth.updateProfile(id, "renamed", null, "wrong", null));
        assertEquals(a.profile(), auth.viewProfile(id));
        assertTrue(sessions.find(token).isPresent());
        String renamed = "renamed-" + UUID.randomUUID();
        var updated = auth.updateProfile(id, renamed, "me@example.com", "password", null);
        assertEquals(renamed, updated.userName()); assertEquals("me@example.com", updated.email());
        assertTrue(users.findByUserName(a.profile().userName()).isEmpty());
        assertTrue(sessions.find(token).isEmpty());
        String replacement = auth.issueRecoveryCode(id, "password");
        assertThrows(IllegalArgumentException.class, () -> auth.recoverCredentials(renamed, a.recoveryCode(), "new"));
        assertNotNull(auth.recoverCredentials(renamed, replacement, "new"));
    }

    @Test void deactivationIsBlockedDuringRaceAndPreservesHistoricalRosterAfterClosure() {
        var a = register(); var b = register(); long id = a.profile().userId();
        long teamId = teams.createTeam(id, "team-" + UUID.randomUUID()).getTeamId();
        long otherTeam = teams.createTeam(b.profile().userId(), "other-" + UUID.randomUUID()).getTeamId();
        var event = new Hackathon(); event.setStatus(HackathonStatus.RUNNING);
        long eventId = events.save(event).getHackathonId();
        var reg = new TeamRegistration(0, teamId, eventId, LocalDateTime.now(), false);
        reg.freezeParticipants(java.util.List.of(id));
        long registrationId = registrations.save(reg).getRegistrationId();
        long inviteId = invitations.save(new Invitation(0, otherTeam, id, InvitationStatus.PENDING)).getInvitationId();
        var token = sessions.create(new SessionPrincipal(SessionPrincipal.ProfileType.USER, id));
        assertThrows(IllegalStateException.class, () -> auth.deactivateProfile(id, "password"));
        assertTrue(users.findById(id).orElseThrow().isActive()); assertTrue(sessions.find(token).isPresent());
        event = events.findById(eventId).orElseThrow(); event.setStatus(HackathonStatus.REVIEW); events.save(event);
        assertThrows(IllegalStateException.class, () -> auth.deactivateProfile(id, "password"));
        event.setStatus(HackathonStatus.CLOSED); events.save(event);
        auth.deactivateProfile(id, "password");
        var removed = users.findById(id).orElseThrow();
        assertFalse(removed.isActive()); assertNull(removed.getTeamId());
        assertEquals(java.util.List.of(id), registrations.findById(registrationId).orElseThrow().getParticipantUserIds());
        assertEquals(InvitationStatus.CANCELLED, invitations.findById(inviteId).orElseThrow().getStatus());
        assertTrue(sessions.find(token).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> auth.viewProfile(id));
        assertThrows(IllegalArgumentException.class, () -> auth.recoverCredentials(a.profile().userName(), a.recoveryCode(), "new"));
        assertNull(auth.login("USER", a.profile().userName(), "password").principalId());
        assertThrows(IllegalArgumentException.class, () -> teams.inviteUser(b.profile().userId(), otherTeam, id));
    }
}
