package it.unicam.hackhub.cli;

import it.unicam.hackhub.controller.AuthController;

public class RegisterCommand implements Command {
    private final AuthController authController;
    private final SessionContext sessionContext;
    private final InputHelper inputHelper;

    public RegisterCommand(AuthController authController, SessionContext sessionContext, InputHelper inputHelper) {
        this.authController = authController;
        this.sessionContext = sessionContext;
        this.inputHelper = inputHelper;
    }

    @Override
    public String name() {
        return "register";
    }

    @Override
    public void execute() {
        try {
            if (sessionContext.isUserLoggedIn() || sessionContext.isStaffLoggedIn()) {
                System.out.println("Sei gia autenticato. Usa logout per cambiare account.");
                return;
            }

            String userName = inputHelper.readNonBlank("Username").trim();
            String password = inputHelper.readNonBlank("Password");

            var result = authController.registerUser(userName, password);
            long userId = result.profile().userId();
            System.out.println("Conserva il codice personale di recupero: " + result.recoveryCode());
            System.out.println("Registrazione completata. userId=" + userId);
        } catch (OperationCancelledException ex) {
            System.out.println("Operazione annullata.");
        } catch (IllegalArgumentException ex) {
            System.out.println("Errore registrazione: " + ex.getMessage());
        }
    }
}
