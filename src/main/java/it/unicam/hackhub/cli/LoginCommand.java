package it.unicam.hackhub.cli;

import it.unicam.hackhub.controller.AuthController;
import it.unicam.hackhub.controller.auth.LoginOutcome;
import it.unicam.hackhub.controller.auth.LoginResult;

public class LoginCommand implements Command {
    private static final int MAX_ATTEMPTS = 3;

    private final AuthController authController;
    private final SessionContext sessionContext;
    private final InputHelper inputHelper;
    private final RegisterCommand registerCommand;

    public LoginCommand(AuthController authController,
                        SessionContext sessionContext,
                        InputHelper inputHelper,
                        RegisterCommand registerCommand) {
        this.authController = authController;
        this.sessionContext = sessionContext;
        this.inputHelper = inputHelper;
        this.registerCommand = registerCommand;
    }

    @Override
    public String name() {
        return "login";
    }

    @Override
    public void execute() {
        try {
            if (sessionContext.isUserLoggedIn() || sessionContext.isStaffLoggedIn()) {
                System.out.println("Sei gia autenticato. Usa logout per cambiare account.");
                return;
            }

            String loginType;
            while (true) {
                loginType = inputHelper.readNonBlank("Tipo login (USER/STAFF)").trim();
                if (isValidLoginType(loginType)) {
                    break;
                }
                System.out.println("Tipo login non valido.");
            }

            int attempts = 0;
            while (attempts < MAX_ATTEMPTS) {
                String identifier = inputHelper.readNonBlank("Identifier").trim();
                String password = inputHelper.readNonBlank("Password");

                attempts++;
                LoginResult result = authController.login(loginType, identifier, password);
                boolean shouldExit = handleResult(loginType, result);
                if (shouldExit) {
                    return;
                }
            }
            System.out.println("Tentativi di login esauriti.");
        } catch (OperationCancelledException ex) {
            System.out.println("Operazione annullata.");
        }
    }

    private boolean handleResult(String loginType, LoginResult result) {
        LoginOutcome outcome = result.outcome();
        switch (outcome) {
            case USER_AUTHENTICATED -> {
                if (result.principalId() != null) {
                    sessionContext.loginUser(result.principalId());
                }
                sessionContext.resetUserProfileNotFoundCount();
                System.out.println("Login user effettuato. userId=" + result.principalId());
                return true;
            }
            case STAFF_AUTHENTICATED -> {
                if (result.principalId() != null) {
                    sessionContext.loginStaff(result.principalId());
                    sessionContext.setStaffRoles(authController.loadStaffRoles(result.principalId()));
                }
                System.out.println("Login staff effettuato. staffId=" + result.principalId());
                return true;
            }
            case USER_NOT_FOUND -> {
                sessionContext.incrementUserProfileNotFoundCount();
                System.out.println("Utente non trovato.");
                if (sessionContext.getUserProfileNotFoundCount() == 3) {
                    String answer;
                    try {
                        answer = inputHelper.readNonBlank("Profilo non trovato 3 volte. Vuoi registrarti? (y/n)").trim();
                    } finally {
                        sessionContext.resetUserProfileNotFoundCount();
                    }
                    if ("y".equalsIgnoreCase(answer)) {
                        registerCommand.execute();
                    }
                    return true;
                }
            }
            case STAFF_NOT_FOUND -> System.out.println("Staff non trovato.");
            case INVALID_PASSWORD -> {
                if ("USER".equalsIgnoreCase(loginType)) {
                    sessionContext.resetUserProfileNotFoundCount();
                }
                System.out.println("Password non valida.");
            }
            case INVALID_INPUT -> System.out.println(result.message());
            case INVALID_LOGIN_TYPE -> System.out.println(result.message());
        }
        return false;
    }

    private boolean isValidLoginType(String loginType) {
        return "USER".equalsIgnoreCase(loginType) || "STAFF".equalsIgnoreCase(loginType);
    }
}
