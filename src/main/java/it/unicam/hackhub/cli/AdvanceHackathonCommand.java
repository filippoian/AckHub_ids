package it.unicam.hackhub.cli;

import it.unicam.hackhub.controller.HackathonController;
import it.unicam.hackhub.model.Hackathon;

import java.util.Optional;

public class AdvanceHackathonCommand implements Command {
    private final HackathonController hackathonController;
    private final SessionContext sessionContext;
    private final InputHelper inputHelper;

    public AdvanceHackathonCommand(HackathonController hackathonController,
                                   SessionContext sessionContext,
                                   InputHelper inputHelper) {
        this.hackathonController = hackathonController;
        this.sessionContext = sessionContext;
        this.inputHelper = inputHelper;
    }

    @Override
    public String name() {
        return "advance-hackathon";
    }

    @Override
    public void execute() {
        try {
            if (!sessionContext.isStaffLoggedIn()) {
                System.out.println("Devi effettuare login STAFF per avanzare lo stato dell'hackathon.");
                return;
            }

            Optional<Long> currentStaffId = sessionContext.getCurrentStaffId();
            if (currentStaffId.isEmpty()) {
                System.out.println("Sessione staff non valida.");
                return;
            }

            long hackathonId = inputHelper.readLong("Hackathon id");
            Hackathon updated = hackathonController.advanceStatus(currentStaffId.get(), hackathonId);
            System.out.println("Hackathon " + updated.getHackathonId() + " avanzato a " + updated.getStatus());
        } catch (OperationCancelledException ex) {
            System.out.println("Operazione annullata.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Impossibile avanzare hackathon: " + ex.getMessage());
        }
    }
}
