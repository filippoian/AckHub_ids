package it.unicam.hackhub;

import it.unicam.hackhub.controller.AuthController;
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
        assertEquals(200, call("GET", "/api/staff/judge/hackathons/"+id+"/warnings", judge, null, false).statusCode());
        assertEquals(200, call("GET", "/api/staff/judge/hackathons/"+id+"/expulsions", judge, null, false).statusCode());
        assertEquals(403, call("GET", "/api/me/profile", mentor, null, false).statusCode());
        long request = requests.save(new SupportRequest(0, team, id, "help", LocalDateTime.now())).getRequestId();
        assertEquals(200, call("POST", "/api/me/support/requests/"+request+"/cancel", token, null, false).statusCode());
        assertTrue(requests.findById(request).orElseThrow().isCancelled());
    }
}
