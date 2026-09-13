package it.unicam.hackhub.cli;

import it.unicam.hackhub.controller.HackathonController;

import java.util.Optional;

public class PayPrizeCommand implements Command {
    private final HackathonController hackathonController;
    private final SessionContext sessionContext;
    private final InputHelper inputHelper;

    public PayPrizeCommand(HackathonController hackathonController,
                           SessionContext sessionContext,
                           InputHelper inputHelper) {
        this.hackathonController = hackathonController;
        this.sessionContext = sessionContext;
        this.inputHelper = inputHelper;
    }

    @Override
    public String name() {
        return "pay-prize";
    }

    @Override
    public void execute() {
        try {
            if (!sessionContext.isStaffLoggedIn()) {
                System.out.println("Devi effettuare login STAFF per erogare il premio.");
                return;
            }

            Optional<Long> currentStaffId = sessionContext.getCurrentStaffId();
            if (currentStaffId.isEmpty()) {
                System.out.println("Sessione staff non valida.");
                return;
            }

            long hackathonId = inputHelper.readLong("Hackathon id");
            String receipt = hackathonController.payPrize(currentStaffId.get(), hackathonId);
            System.out.println(receipt);
        } catch (OperationCancelledException ex) {
            System.out.println("Operazione annullata.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Errore erogazione premio: " + ex.getMessage());
        }
    }
}
