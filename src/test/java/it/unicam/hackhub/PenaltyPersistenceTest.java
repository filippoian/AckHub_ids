package it.unicam.hackhub;

import it.unicam.hackhub.controller.EvaluationController;
import it.unicam.hackhub.controller.ViolationReportController;
import it.unicam.hackhub.model.*;
import it.unicam.hackhub.model.enums.*;
import it.unicam.hackhub.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PenaltyPersistenceTest {
    @Autowired EvaluationController evaluationController;
    @Autowired ViolationReportController violationController;
    @Autowired HackathonRepository events;
    @Autowired StaffAssignmentRepository assignments;
    @Autowired TeamRegistrationRepository registrations;
    @Autowired SubmissionRepository submissions;
    @Autowired EvaluationRepository evaluations;
    @Autowired ViolationReportRepository reports;
    @Autowired EntityManager em;

    @Test void warningDecisionAndRecalculationSurviveReload() {
        Hackathon event = new Hackathon();
        event.setStatus(HackathonStatus.REVIEW);
        event = events.save(event);
        long eventId = event.getHackathonId();
        assignments.save(new StaffAssignment(0, 700, eventId, StaffRole.JUDGE));
        assignments.save(new StaffAssignment(0, 701, eventId, StaffRole.ORGANIZER));
        var registration = registrations.save(new TeamRegistration(0, 800, eventId, LocalDateTime.now(), false));
        var submission = submissions.save(new Submission(0, registration.getRegistrationId(), "contenuto",
                LocalDateTime.now(), LocalDateTime.now()));
        var report = reports.save(new ViolationReport(0, eventId, 800, 702, "violazione", LocalDateTime.now(), null));
        long reportId = report.getReportId();
        violationController.manageReport(701, reportId, "WARN");
        evaluationController.evaluateWarning(700, reportId, 7);
        evaluationController.evaluateSubmission(700, submission.getSubmissionId(), 30, "giudizio");
        em.flush(); em.clear();
        var loaded = evaluations.findBySubmissionId(submission.getSubmissionId()).orElseThrow();
        assertEquals(30, loaded.getBaseScore()); assertEquals(7, loaded.getPenalty()); assertEquals(23, loaded.getScore());
        var loadedReport = reports.findById(reportId).orElseThrow();
        assertTrue(loadedReport.isPenaltyEvaluated());
        assertEquals(701L, loadedReport.getManagedByStaffId());
        assertEquals(700L, loadedReport.getPenaltyJudgeStaffId());
        evaluationController.evaluateWarning(700, reportId, 0);
        em.flush(); em.clear();
        assertEquals(30, evaluations.findBySubmissionId(submission.getSubmissionId()).orElseThrow().getScore());
        assertTrue(reports.findById(reportId).orElseThrow().isPenaltyEvaluated());
    }
}
