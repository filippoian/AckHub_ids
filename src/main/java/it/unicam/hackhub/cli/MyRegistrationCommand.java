package it.unicam.hackhub.cli;

import it.unicam.hackhub.controller.TeamController;
import it.unicam.hackhub.controller.TeamRegistrationController;
import it.unicam.hackhub.model.TeamRegistration;

import java.util.Optional;

public class MyRegistrationCommand implements Command {
    private final TeamRegistrationController teamRegistrationController;
    private final TeamController teamController;
    private final SessionContext sessionContext;

    public MyRegistrationCommand(TeamRegistrationController teamRegistrationController,
                                 TeamController teamController,
                                 SessionContext sessionContext) {
        this.teamRegistrationController = teamRegistrationController;
        this.teamController = teamController;
        this.sessionContext = sessionContext;
    }

    @Override
    public String name() {
        return "my-registration";
    }

    @Override
    public void execute() {
        try {
            if (!sessionContext.isUserLoggedIn()) {
                System.out.println("Devi effettuare login USER per vedere la tua registrazione.");
                return;
            }

            Optional<Long> currentUserId = sessionContext.getCurrentUserId();
            if (currentUserId.isEmpty()) {
                System.out.println("Sessione utente non valida.");
                return;
            }

            Long teamId = teamController.getTeamIdOfUser(currentUserId.get());
            if (teamId == null) {
                System.out.println("Non hai un team. Crea un team prima di registrarti a un hackathon.");
                return;
            }

            Optional<TeamRegistration> registrationOpt = teamRegistrationController.getMyRegistration(currentUserId.get());
            if (registrationOpt.isEmpty()) {
                System.out.println("Nessuna registrazione trovata per il tuo team.");
                return;
            }

            TeamRegistration registration = registrationOpt.get();
            System.out.println(
                    "registrationId=" + registration.getRegistrationId()
                            + ", hackathonId=" + registration.getHackathonId()
                            + ", isExpelled=" + registration.isExpelled()
            );
        } catch (OperationCancelledException ex) {
            System.out.println("Operazione annullata.");
        } catch (IllegalArgumentException ex) {
            System.out.println("Errore lettura registrazione: " + ex.getMessage());
        }
    }
}
