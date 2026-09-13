package it.unicam.hackhub.cli;

import it.unicam.hackhub.controller.TeamRegistrationController;
import it.unicam.hackhub.model.TeamRegistration;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class RegisterTeamCommand implements Command {
    private final TeamRegistrationController teamRegistrationController;
    private final SessionContext sessionContext;
    private final InputHelper inputHelper;

    public RegisterTeamCommand(TeamRegistrationController teamRegistrationController,
                               SessionContext sessionContext,
                               InputHelper inputHelper) {
        this.teamRegistrationController = teamRegistrationController;
        this.sessionContext = sessionContext;
        this.inputHelper = inputHelper;
    }

    @Override
    public String name() {
        return "register-team";
    }

    @Override
    public void execute() {
        try {
            if (!sessionContext.isUserLoggedIn()) {
                System.out.println("Devi effettuare login USER per registrare il team.");
                return;
            }

            Optional<Long> currentUserId = sessionContext.getCurrentUserId();
            if (currentUserId.isEmpty()) {
                System.out.println("Sessione utente non valida.");
                return;
            }

            List<TeamRegistrationController.HackathonRegistrationOption> availableHackathons =
                    teamRegistrationController.listRegisterableHackathons(currentUserId.get());
            if (availableHackathons.isEmpty()) {
                System.out.println("Nessun hackathon disponibile per l'iscrizione.");
                return;
            }

            Set<Long> selectableHackathonIds = new LinkedHashSet<>();
            List<List<String>> rows = new ArrayList<>();
            for (TeamRegistrationController.HackathonRegistrationOption hackathon : availableHackathons) {
                selectableHackathonIds.add(hackathon.hackathonId());
                rows.add(List.of(
                        String.valueOf(hackathon.hackathonId()),
                        safe(hackathon.name()),
                        safe(hackathon.status()),
                        safe(hackathon.location()),
                        String.valueOf(hackathon.registrationDeadline())
                ));
            }

            TablePrinter.print(List.of("HACKATHON_ID", "NAME", "STATUS", "LOCATION", "REG_DEADLINE"), rows);
            long selectedHackathonId = readHackathonId(selectableHackathonIds);
            TeamRegistration registration = teamRegistrationController
                    .registerTeamToHackathon(currentUserId.get(), selectedHackathonId);

            System.out.println(
                    "Team " + registration.getTeamId()
                            + " registrato a Hackathon " + registration.getHackathonId()
                            + " (registrationId=" + registration.getRegistrationId() + ")"
            );
        } catch (OperationCancelledException ex) {
            System.out.println("Operazione annullata.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Errore registrazione team: " + ex.getMessage());
        }
    }

    private long readHackathonId(Set<Long> selectableHackathonIds) {
        while (true) {
            long hackathonId = inputHelper.readLong("Hackathon id");
            if (selectableHackathonIds.contains(hackathonId)) {
                return hackathonId;
            }
            System.out.println("Hackathon non valido.");
        }
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }
}
