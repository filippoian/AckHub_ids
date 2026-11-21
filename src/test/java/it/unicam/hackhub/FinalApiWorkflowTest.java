package it.unicam.hackhub;

import it.unicam.hackhub.web.session.*;
import it.unicam.hackhub.model.*;
import it.unicam.hackhub.model.enums.*;
import it.unicam.hackhub.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:api_alignment_test;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class FinalApiWorkflowTest {
    @Value("${local.server.port}") int port;
    @Autowired ObjectMapper json;
    @Autowired InMemorySessionStore sessions;
    @Autowired HackathonRepository events;
    @Autowired StaffAssignmentRepository assignments;
    @Autowired TeamRepository teams;
    @Autowired UserRepository users;
    @Autowired TeamRegistrationRepository registrations;
    @Autowired ViolationReportRepository reports;
    @Autowired SupportRequestRepository requests;
    final HttpClient client = HttpClient.newHttpClient();

    HttpResponse<String> call(String method, String path, String token, Map<String, ?> data, boolean form) throws Exception {
        String body = data == null ? "" : form ? data.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(e.getValue().toString(), StandardCharsets.UTF_8))
                .collect(java.util.stream.Collectors.joining("&")) : json.writeValueAsString(data);
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:"+port+path))
                .header("Content-Type", form ? "application/x-www-form-urlencoded" : "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(body));
        if (token != null) builder.header("X-Session-Token", token);
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test void registrationProfileAndRecoveryWorkOverHttpWithoutExposingHashes() throws Exception {
        String name = "api-"+UUID.randomUUID();
        var created = call("POST", "/api/auth/register", null, Map.of("username", name, "password", "secret", "email", "me@example.com"), false);
        assertEquals(201, created.statusCode(), created.body());
        var registration = json.readTree(created.body());
        String recovery = registration.path("recoveryCode").asText();
        assertFalse(recovery.isBlank()); assertEquals("me@example.com", registration.path("profile").path("email").asText());
        assertFalse(created.body().contains("Hash"));
        var login = call("POST", "/api/auth/login", null, Map.of("type", "USER", "identifier", name, "password", "secret"), false);
        assertEquals(200, login.statusCode(), login.body()); String token = json.readTree(login.body()).path("token").asText();
        assertEquals(401, call("GET", "/api/me/profile", null, null, false).statusCode());
        var profile = call("GET", "/api/me/profile", token, null, false);
        assertEquals(200, profile.statusCode()); assertFalse(profile.body().contains("Hash")); assertFalse(profile.body().contains("recoveryCode"));
        String renamed = name + "-changed";
        var updated = call("POST", "/api/me/profile", token, Map.of("userName", renamed, "currentPassword", "secret", "email", "updated@example.com"), true);
        assertEquals(200, updated.statusCode(), updated.body());
        assertEquals(401, call("GET", "/api/me/profile", token, null, false).statusCode());
        var recovered = call("POST", "/api/auth/recover", null, Map.of("userName", renamed, "recoveryCode", recovery, "newPassword", "changed"), true);
        assertEquals(200, recovered.statusCode(), recovered.body());
        assertNotEquals(recovery, json.readTree(recovered.body()).path("recoveryCode").asText());
        assertEquals(400, call("POST", "/api/auth/recover", null, Map.of("userName", renamed, "recoveryCode", recovery, "newPassword", "again"), true).statusCode());
        login = call("POST", "/api/auth/login", null, Map.of("type", "USER", "identifier", renamed, "password", "changed"), false);
        token = json.readTree(login.body()).path("token").asText();
        assertEquals(200, call("POST", "/api/me/profile/deactivate", token, Map.of("currentPassword", "changed"), true).statusCode());
        assertEquals(401, call("GET", "/api/me/profile", token, null, false).statusCode());
        assertEquals(400, call("POST", "/api/auth/login", null, Map.of("type", "USER", "identifier", renamed, "password", "changed"), false).statusCode());
    }

    @Test void submissionAndEvaluationKeepCreationAndUpdateSeparate() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        Hackathon event = new Hackathon();
        event.setStatus(HackathonStatus.RUNNING);
        event.setStartDate(now.minusHours(1));
        event.setEndDate(now.plusHours(1));
        event.setSubmissionDeadline(event.getEndDate());
        long hackathonId = events.save(event).getHackathonId();
        assignments.save(new StaffAssignment(0, 6611, hackathonId, StaffRole.JUDGE));
        assignments.save(new StaffAssignment(0, 6612, hackathonId, StaffRole.ORGANIZER));

        Team team = teams.save(new Team(0, "submission-" + UUID.randomUUID()));
        long userId = users.save(new User(0, "submitter-" + UUID.randomUUID(), "hash", team.getTeamId())).getUserId();
        registrations.save(new TeamRegistration(0, team.getTeamId(), hackathonId, now, false));
        String user = sessions.create(new SessionPrincipal(SessionPrincipal.ProfileType.USER, userId));
        String judge = sessions.create(new SessionPrincipal(SessionPrincipal.ProfileType.STAFF, 6611));
        String organizer = sessions.create(new SessionPrincipal(SessionPrincipal.ProfileType.STAFF, 6612));

        String submissionPath = "/api/me/submission";
        assertEquals(409, call("PUT", submissionPath, user, Map.of("content", "too soon"), false).statusCode());
        var posted = call("POST", submissionPath, user, Map.of("content", "prima consegna"), false);
        assertEquals(200, posted.statusCode(), posted.body());
        var original = json.readTree(call("GET", submissionPath, user, null, false).body());
        long submissionId = original.path("submissionId").asLong();
        assertEquals(409, call("POST", submissionPath, user, Map.of("content", "duplicato"), false).statusCode());
        var updated = call("PUT", submissionPath, user, Map.of("content", "consegna aggiornata"), false);
        assertEquals(200, updated.statusCode(), updated.body());
        assertEquals(submissionId, json.readTree(updated.body()).path("submissionId").asLong());
        assertEquals(original.path("submittedAt"), json.readTree(updated.body()).path("submittedAt"));

        event = events.findById(hackathonId).orElseThrow();
        event.setStatus(HackathonStatus.REVIEW);
        events.save(event);
        String submissionsPath = "/api/staff/hackathons/" + hackathonId + "/submissions";
        var summary = json.readTree(call("GET", submissionsPath, judge, null, false).body()).get(0);
        assertEquals(team.getTeamName(), summary.path("teamName").asText());
        assertEquals("NONE", summary.path("disciplinaryStatus").asText());
        assertFalse(summary.path("evaluated").asBoolean());

        String evaluationPath = submissionsPath + "/" + submissionId + "/evaluation";
        var initial = Map.of("score", 20, "comment", "giudizio");
        assertEquals(409, call("PUT", evaluationPath, judge, initial, false).statusCode());
        assertEquals(400, call("POST", evaluationPath, judge, Map.of("comment", "manca il voto"), false).statusCode());
        var evaluated = call("POST", evaluationPath, judge, initial, false);
        assertEquals(200, evaluated.statusCode(), evaluated.body());
        assertEquals(409, call("POST", evaluationPath, judge, initial, false).statusCode());
        var revised = call("PUT", evaluationPath, judge, Map.of("score", 25), false);
        assertEquals(200, revised.statusCode(), revised.body());
        assertEquals(json.readTree(evaluated.body()).path("evaluationId"), json.readTree(revised.body()).path("evaluationId"));
        assertEquals(25, json.readTree(revised.body()).path("baseScore").asInt());
        assertEquals("giudizio", json.readTree(revised.body()).path("comment").asText());

        String declarationPath = "/api/staff/organizer/hackathons/" + hackathonId + "/proclamation";
        var preview = call("GET", declarationPath, organizer, null, false);
        assertEquals(200, preview.statusCode(), preview.body());
        assertTrue(json.readTree(preview.body()).path("canProclaim").asBoolean());
        assertTrue(json.readTree(preview.body()).path("teams").get(0).path("eligible").asBoolean());
        assertEquals(400, call("GET", declarationPath, judge, null, false).statusCode());
    }

    @Test void newEventReportAndSupportEndpointsUseSessionIdentity() throws Exception {
        var event = new Hackathon(); event.setHackathonName("http-"+UUID.randomUUID());
        event.setStatus(HackathonStatus.REGISTRATION); event.setRegistrationDeadline(LocalDateTime.now().plusDays(1));
        event.setStartDate(LocalDateTime.now().plusDays(2)); event.setEndDate(LocalDateTime.now().plusDays(3));
        long id = events.save(event).getHackathonId();
        assignments.save(new StaffAssignment(0, 6601, id, StaffRole.ORGANIZER));
        String organizer = sessions.create(new SessionPrincipal(SessionPrincipal.ProfileType.STAFF, 6601));
        String outsider = sessions.create(new SessionPrincipal(SessionPrincipal.ProfileType.STAFF, 6609));
        var payload = new LinkedHashMap<String, Object>();
        payload.put("name", event.getHackathonName()); payload.put("regulation", "new rules");
        payload.put("registrationDeadline", event.getRegistrationDeadline().toString());
        payload.put("startDate", event.getStartDate().toString()); payload.put("endDate", event.getEndDate().toString());
        payload.put("submissionDeadline", event.getEndDate().toString()); payload.put("location", "Camerino");
        payload.put("prizeAmount", "12.34"); payload.put("maxTeamSize", 4);
        payload.put("judgeId", 6603); payload.put("mentorIds", List.of(6602));
        assertEquals(400, call("PUT", "/api/staff/organizer/hackathons/"+id, outsider, payload, false).statusCode());
        var update = call("PUT", "/api/staff/organizer/hackathons/"+id, organizer, payload, false);
        assertEquals(200, update.statusCode(), update.body());
        assertEquals(200, call("DELETE", "/api/staff/organizer/hackathons/"+id, organizer, null, false).statusCode());
        assertTrue(events.findById(id).isEmpty());

        event = new Hackathon(); event.setStatus(HackathonStatus.RUNNING); event.setStartDate(LocalDateTime.now().minusHours(1)); event.setEndDate(LocalDateTime.now().plusHours(2));
        id = events.save(event).getHackathonId();
        assignments.save(new StaffAssignment(0, 6602, id, StaffRole.MENTOR));
        assignments.save(new StaffAssignment(0, 6603, id, StaffRole.JUDGE));
        assignments.save(new StaffAssignment(0, 6601, id, StaffRole.ORGANIZER));
        assignments.save(new StaffAssignment(0, 6609, id, StaffRole.MENTOR));
        String mentor = sessions.create(new SessionPrincipal(SessionPrincipal.ProfileType.STAFF, 6602));
        String judge = sessions.create(new SessionPrincipal(SessionPrincipal.ProfileType.STAFF, 6603));
        long team = teams.save(new Team(0, "http-team-"+UUID.randomUUID())).getTeamId();
        long user = users.save(new User(0, "http-user-"+UUID.randomUUID(), "hash", team)).getUserId();
        String token = sessions.create(new SessionPrincipal(SessionPrincipal.ProfileType.USER, user));
        registrations.save(new TeamRegistration(0, team, id, LocalDateTime.now(), false));
        long report = reports.save(new ViolationReport(0, id, team, 6602, "first", LocalDateTime.now(), null)).getReportId();
        assertEquals(200, call("POST", "/api/staff/mentor/reports/"+report+"/update", mentor, Map.of("description", "fixed"), true).statusCode());
        assertEquals("fixed", reports.findById(report).orElseThrow().getDescription());
        assertEquals(200, call("POST", "/api/staff/mentor/reports/"+report+"/withdraw", mentor, null, false).statusCode());
        assertEquals("WITHDRAWN", reports.findById(report).orElseThrow().getDecision());
        String history = "/api/staff/mentor/hackathons/" + id + "/violation-reports";
        var mine = call("GET", history, mentor, null, false);
        assertEquals(200, mine.statusCode(), mine.body());
        assertEquals("WITHDRAWN", json.readTree(mine.body()).get(0).path("decision").asText());
        assertTrue(json.readTree(call("GET", history, outsider, null, false).body()).isEmpty());
        assertEquals(400, call("GET", history + "/" + report, outsider, null, false).statusCode());
        String organizerReports = "/api/staff/organizer/hackathons/" + id + "/violation-reports";
        assertEquals(1, json.readTree(call("GET", organizerReports, organizer, null, false).body()).size());
        assertTrue(json.readTree(call("GET", organizerReports + "?onlyPending=true", organizer, null, false).body()).isEmpty());
        assertEquals(200, call("GET", "/api/staff/judge/hackathons/"+id+"/warnings", judge, null, false).statusCode());
        assertEquals(200, call("GET", "/api/staff/judge/hackathons/"+id+"/expulsions", judge, null, false).statusCode());
        assertEquals(403, call("GET", "/api/me/profile", mentor, null, false).statusCode());
        long request = requests.save(new SupportRequest(0, team, id, "help", LocalDateTime.now())).getRequestId();
        assertEquals(200, call("POST", "/api/me/support/requests/"+request+"/cancel", token, null, false).statusCode());
        assertTrue(requests.findById(request).orElseThrow().isCancelled());

        long bookableRequest = requests.save(new SupportRequest(0, team, id, "book", LocalDateTime.now())).getRequestId();
        String staffRequest = "/api/staff/mentor/hackathons/" + id + "/support-requests/" + bookableRequest;
        var proposal = call("POST", "/api/staff/mentor/support-requests/" + bookableRequest + "/call-proposals", mentor,
                Map.of("proposedStart", LocalDateTime.now().plusMinutes(30).toString(),
                        "proposedEnd", LocalDateTime.now().plusMinutes(50).toString()), false);
        assertEquals(201, proposal.statusCode(), proposal.body());
        long proposalId = json.readTree(proposal.body()).path("proposalId").asLong();
        var detail = call("GET", staffRequest, mentor, null, false);
        assertEquals(proposalId, json.readTree(detail.body()).path("proposals").get(0).path("proposalId").asLong());
        var booking = call("POST", "/api/me/calls/proposals/" + proposalId + "/book", token, null, false);
        assertEquals(201, booking.statusCode(), booking.body());
        long callId = json.readTree(booking.body()).path("callId").asLong();
        assertTrue(callId > 0);
        String memberRequest = "/api/me/support/requests/" + bookableRequest;
        var cancel = call("POST", memberRequest + "/cancel", token, null, false);
        assertEquals(409, cancel.statusCode(), cancel.body());
        assertEquals(callId, json.readTree(cancel.body()).path("bookings").get(0).path("callId").asLong());
        assertTrue(json.readTree(call("GET", memberRequest + "/call-proposals", token, null, false).body()).isEmpty());

    }
}
