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
import it.unicam.hackhub.model.enums.StaffRole;

import java.util.EnumSet;
import java.util.Set;
import java.util.Scanner;

public class AppContext {
    private final AuthController authController;
    private final TeamController teamController;
    private final HackathonController hackathonController;
    private final MentorManagementController mentorManagementController;
    private final TeamRegistrationController teamRegistrationController;
    private final SubmissionController submissionController;
    private final SubmissionAccessController submissionAccessController;
    private final EvaluationController evaluationController;
    private final SupportController supportController;
    private final ViolationReportController violationReportController;

    private final SessionContext sessionContext;

    public AppContext(AuthController authController,
                      TeamController teamController,
                      HackathonController hackathonController,
                      MentorManagementController mentorManagementController,
                      TeamRegistrationController teamRegistrationController,
                      SubmissionController submissionController,
                      SubmissionAccessController submissionAccessController,
                      EvaluationController evaluationController,
                      SupportController supportController,
                      ViolationReportController violationReportController) {
        this.authController = authController;
        this.teamController = teamController;
        this.hackathonController = hackathonController;
        this.mentorManagementController = mentorManagementController;
        this.teamRegistrationController = teamRegistrationController;
        this.submissionController = submissionController;
        this.submissionAccessController = submissionAccessController;
        this.evaluationController = evaluationController;
        this.supportController = supportController;
        this.violationReportController = violationReportController;
        this.sessionContext = new SessionContext();
        authController.registerSessionInvalidator(this.sessionContext);
    }

    // Hook lasciato per eventuale inizializzazione CLI.
    public void init() {
    }

    // Costruisce e registra tutti i comandi disponibili in CLI.
    public CommandRegistry createCommandRegistry(Scanner scanner) {
        CommandRegistry registry = new CommandRegistry();
        InputHelper inputHelper = new InputHelper(scanner);

        RegisterCommand registerCommand = new RegisterCommand(authController, sessionContext, inputHelper);
        LoginCommand loginCommand = new LoginCommand(
                authController,
                sessionContext,
                inputHelper,
                registerCommand
        );
        LogoutCommand logoutCommand = new LogoutCommand(sessionContext);

        // Definisce i gruppi di accesso usati dai comandi.
        EnumSet<CommandRegistry.Audience> guestOnly = EnumSet.of(CommandRegistry.Audience.GUEST);
        EnumSet<CommandRegistry.Audience> userOnly = EnumSet.of(CommandRegistry.Audience.USER);
        EnumSet<CommandRegistry.Audience> staffOnly = EnumSet.of(CommandRegistry.Audience.STAFF);
        EnumSet<CommandRegistry.Audience> userAndStaff = EnumSet.of(
                CommandRegistry.Audience.USER,
                CommandRegistry.Audience.STAFF
        );
        EnumSet<CommandRegistry.Audience> allAudiences = EnumSet.of(
                CommandRegistry.Audience.GUEST,
                CommandRegistry.Audience.USER,
                CommandRegistry.Audience.STAFF
        );

        registry.register(registerCommand, guestOnly, Set.of());
        registry.register(loginCommand, guestOnly, Set.of());
        registry.register(logoutCommand, userAndStaff, Set.of());
        registry.register(new ListHackathonsCommand(hackathonController), allAudiences, Set.of());
        registry.register(new ViewHackathonCommand(hackathonController, inputHelper), allAudiences, Set.of());
        registry.register(
                new CreateHackathonCommand(hackathonController, sessionContext, inputHelper),
                staffOnly,
                Set.of(StaffRole.ORGANIZER)
        );
        registry.register(
                new AddMentorCommand(
                        mentorManagementController,
                        sessionContext,
                        inputHelper
                ),
                staffOnly,
                Set.of(StaffRole.ORGANIZER)
        );

        registry.register(
                new AdvanceHackathonCommand(hackathonController, sessionContext, inputHelper),
                staffOnly,
                Set.of(StaffRole.ORGANIZER)
        );
        registry.register(
                new SetWinnerCommand(hackathonController, sessionContext, inputHelper),
                staffOnly,
                Set.of(StaffRole.ORGANIZER)
        );
        registry.register(
                new PayPrizeCommand(hackathonController, sessionContext, inputHelper),
                staffOnly,
                Set.of(StaffRole.ORGANIZER)
        );
        registry.register(
                new ProclaimWinnerCommand(hackathonController, sessionContext, inputHelper),
                staffOnly,
                Set.of(StaffRole.ORGANIZER)
        );
        registry.register(
                new ListSubmissionsCommand(
                        submissionAccessController,
                        sessionContext,
                        inputHelper
                ),
                staffOnly,
                Set.of()
        );
        registry.register(
                new EvaluateSubmissionCommand(
                        submissionAccessController,
                        evaluationController,
                        sessionContext,
                        inputHelper
                ),
                staffOnly,
                Set.of(StaffRole.JUDGE)
        );
        registry.register(new EvaluateWarningCommand(evaluationController, violationReportController,
                sessionContext, inputHelper), staffOnly, Set.of(StaffRole.JUDGE));
        registry.register(new ViewEvaluationCommand(
                submissionAccessController,
                evaluationController,
                sessionContext,
                inputHelper
        ), staffOnly, Set.of());
        registry.register(
                new CreateSupportRequestCommand(
                        supportController,
                        sessionContext,
                        inputHelper
                ),
                userOnly,
                Set.of()
        );
        registry.register(
                new CreateCallProposalCommand(supportController, sessionContext, inputHelper),
                staffOnly,
                Set.of(StaffRole.MENTOR)
        );
        registry.register(
                new ListSupportRequestsCommand(supportController, sessionContext, inputHelper),
                staffOnly,
                Set.of(StaffRole.MENTOR)
        );
        registry.register(
                new ListCallProposalsCommand(
                        supportController,
                        sessionContext
                ),
                userOnly,
                Set.of()
        );
        registry.register(
                new BookCallCommand(
                        supportController,
                        sessionContext,
                        inputHelper
                ),
                userOnly,
                Set.of()
        );
        registry.register(
                new CreateReportCommand(
                        violationReportController,
                        sessionContext,
                        inputHelper
                ),
                staffOnly,
                Set.of(StaffRole.MENTOR)
        );
        registry.register(
                new ListReportsCommand(
                        violationReportController,
                        sessionContext,
                        inputHelper
                ),
                staffOnly,
                Set.of(StaffRole.ORGANIZER)
        );
        registry.register(
                new ManageReportCommand(violationReportController, sessionContext, inputHelper),
                staffOnly,
                Set.of(StaffRole.ORGANIZER)
        );
        registry.register(new RegisterTeamCommand(
                teamRegistrationController,
                sessionContext,
                inputHelper
        ), userOnly, Set.of());
        registry.register(new MyRegistrationCommand(teamRegistrationController, teamController, sessionContext), userOnly, Set.of());
        registry.register(new SubmitCommand(submissionController, teamController, sessionContext, inputHelper), userOnly, Set.of());
        registry.register(
                new UpdateSubmissionCommand(
                        submissionController,
                        teamController,
                        sessionContext,
                        inputHelper
                ),
                userOnly,
                Set.of()
        );
        registry.register(new MySubmissionCommand(submissionController, teamController, sessionContext), userOnly, Set.of());
        registry.register(new CreateTeamCommand(teamController, sessionContext, inputHelper), userOnly, Set.of());
        registry.register(new InviteUserCommand(teamController, sessionContext, inputHelper), userOnly, Set.of());
        registry.register(
                new ViewInvitesCommand(
                        teamController,
                        sessionContext,
                        inputHelper
                ),
                userOnly,
                Set.of()
        );

        registry.register(new InviteUsersCommand(teamController, sessionContext, inputHelper), userOnly, Set.of());
        registry.register(new CancelInvitationCommand(teamController, sessionContext, inputHelper), userOnly, Set.of());
        registry.register(new LeaveTeamCommand(teamController, sessionContext, inputHelper), userOnly, Set.of());
        return registry;
    }

    public AuthController getAuthController() { return authController; }
    public TeamController getTeamController() { return teamController; }
    public HackathonController getHackathonController() { return hackathonController; }
    public TeamRegistrationController getTeamRegistrationController() { return teamRegistrationController; }
    public SubmissionController getSubmissionController() { return submissionController; }
    public EvaluationController getEvaluationController() { return evaluationController; }
    public SupportController getSupportController() { return supportController; }
    public SessionContext getSessionContext() { return sessionContext; }
}
