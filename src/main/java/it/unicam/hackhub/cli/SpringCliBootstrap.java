package it.unicam.hackhub.cli;

import it.unicam.hackhub.controller.AuthController;
import it.unicam.hackhub.controller.EvaluationController;
import it.unicam.hackhub.controller.HackathonController;
import it.unicam.hackhub.controller.MentorManagementController;
import it.unicam.hackhub.controller.SubmissionAccessController;
import it.unicam.hackhub.controller.SubmissionController;
import it.unicam.hackhub.controller.SupportController;
import it.unicam.hackhub.controller.TeamController;
import it.unicam.hackhub.controller.TeamRegistrationController;
import it.unicam.hackhub.controller.ViolationReportController;
import java.util.Scanner;
import org.springframework.stereotype.Component;

@Component
public class SpringCliBootstrap {
    private final AppContext appContext;

    public SpringCliBootstrap(AuthController authController,
                              TeamController teamController,
                              TeamRegistrationController teamRegistrationController,
                              SubmissionController submissionController,
                              SupportController supportController,
                              EvaluationController evaluationController,
                              HackathonController hackathonController,
                              ViolationReportController violationReportController,
                              MentorManagementController mentorManagementController,
                              SubmissionAccessController submissionAccessController) {
        this.appContext = new AppContext(
                authController,
                teamController,
                hackathonController,
                mentorManagementController,
                teamRegistrationController,
                submissionController,
                submissionAccessController,
                evaluationController,
                supportController,
                violationReportController
        );
    }

    // Avvia menu CLI usando gli stessi bean Spring dell'app.
    public void runCli() {
        appContext.init();
        Scanner scanner = new Scanner(System.in);
        CommandRegistry registry = appContext.createCommandRegistry(scanner);
        MainMenu mainMenu = new MainMenu(registry, scanner, appContext.getSessionContext());
        mainMenu.run();
    }
}
