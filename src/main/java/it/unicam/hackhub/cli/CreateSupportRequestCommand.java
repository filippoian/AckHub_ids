package it.unicam.hackhub.cli;

import it.unicam.hackhub.controller.SupportController;
import it.unicam.hackhub.model.SupportRequest;

import java.util.Optional;

public class CreateSupportRequestCommand implements Command {
    private final SupportController supportController;
    private final SessionContext sessionContext;
    private final InputHelper inputHelper;

    public CreateSupportRequestCommand(SupportController supportController,
                                       SessionContext sessionContext,
                                       InputHelper inputHelper) {
        this.supportController = supportController;
        this.sessionContext = sessionContext;
        this.inputHelper = inputHelper;
    }

    @Override
    public String name() {
        return "create-support-request";
    }

    @Override
    public void execute() {
        try {
            if (!sessionContext.isUserLoggedIn()) {
                System.out.println("Devi effettuare login USER per creare una richiesta di supporto.");
                return;
            }

            Optional<Long> currentUserId = sessionContext.getCurrentUserId();
            if (currentUserId.isEmpty()) {
                System.out.println("Sessione utente non valida.");
                return;
            }

            String message = inputHelper.readNonBlank("Messaggio");
            SupportRequest request = supportController.createSupportRequestForCurrentUser(currentUserId.get(), message);
            System.out.println("Richiesta creata. requestId=" + request.getRequestId());
        } catch (OperationCancelledException ex) {
            System.out.println("Operazione annullata.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Errore creazione richiesta supporto: " + ex.getMessage());
        }
    }
}
