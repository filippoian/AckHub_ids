package it.unicam.hackhub.cli;

public class LogoutCommand implements Command {
    private final SessionContext sessionContext;

    public LogoutCommand(SessionContext sessionContext) {
        this.sessionContext = sessionContext;
    }

    @Override
    public String name() {
        return "logout";
    }

    @Override
    public void execute() {
        if (!sessionContext.isUserLoggedIn() && !sessionContext.isStaffLoggedIn()) {
            System.out.println("Nessuna sessione attiva.");
            return;
        }
        sessionContext.logout();
        System.out.println("Logout effettuato.");
    }
}
