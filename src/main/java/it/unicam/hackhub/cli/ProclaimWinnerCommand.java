package it.unicam.hackhub.cli;

import it.unicam.hackhub.controller.HackathonController;
import it.unicam.hackhub.model.Hackathon;

import java.util.List;
import java.util.Optional;

public class ProclaimWinnerCommand implements Command {
    private final HackathonController hackathonController;
    private final SessionContext sessionContext;
    private final InputHelper inputHelper;

    public ProclaimWinnerCommand(HackathonController hackathonController,
                                 SessionContext sessionContext,
                                 InputHelper inputHelper) {
        this.hackathonController = hackathonController;
        this.sessionContext = sessionContext;
        this.inputHelper = inputHelper;
    }

    @Override
    public String name() {
        return "proclaim-winner";
    }

    @Override
    public void execute() {
        try {
            if (!sessionContext.isStaffLoggedIn()) {
                System.out.println("Devi effettuare login STAFF per proclamare il vincitore.");
                return;
            }

            Optional<Long> currentStaffId = sessionContext.getCurrentStaffId();
            if (currentStaffId.isEmpty()) {
                System.out.println("Sessione staff non valida.");
                return;
            }

            long hackathonId = inputHelper.readLong("Hackathon id");
            var preview = hackathonController.viewProclamation(currentStaffId.get(), hackathonId);
            if (preview.winnerTeamId() != null) {
                System.out.println("Vincitore già proclamato: team " + preview.winnerTeamId());
                if (preview.prizePaid()) {
                    System.out.println("Premio già erogato. Ricevuta: " + preview.receiptId());
                } else if ("s".equalsIgnoreCase(inputHelper.readNonBlank("Ritentare l’erogazione? (s/n)").trim())) {
                    System.out.println(hackathonController.payPrize(currentStaffId.get(), hackathonId));
                }
                return;
            }

            var rows = preview.teams().stream()
                    .map(team -> List.of(String.valueOf(team.teamId()),
                            team.teamName() == null ? "-" : team.teamName(),
                            team.expelled() ? "Espulso" : team.isEligible() ? "Candidato" : "Non candidabile",
                            team.submissionId() == null ? "-" : team.submissionId().toString(),
                            team.finalScore() == null ? "-" : team.finalScore().toString(),
                            team.pendingWarnings().toString()))
                    .toList();
            TablePrinter.print(List.of("TEAM_ID", "TEAM", "STATO", "SOTTOMISSIONE", "VOTO", "PENALITÀ MANCANTI"), rows);
            if (!preview.canProclaim()) {
                System.out.println("Proclamazione non disponibile. Controlla fase, candidati, valutazioni e penalità.");
                return;
            }

            long teamId = inputHelper.readLong("Team vincitore");
            if (preview.teams().stream().noneMatch(team -> team.teamId() == teamId && team.isEligible())) {
                System.out.println("Il team selezionato non è candidabile.");
                return;
            }
            if (!"s".equalsIgnoreCase(inputHelper.readNonBlank("Confermi la proclamazione? (s/n)").trim())) {
                return;
            }

            Hackathon declared = hackathonController.setWinner(currentStaffId.get(), hackathonId, teamId);
            System.out.println("Vincitore proclamato: team " + declared.getWinnerTeamId());
            System.out.println(hackathonController.payPrize(currentStaffId.get(), hackathonId));
            Hackathon result = hackathonController.getHackathonDetails(hackathonId).orElseThrow();
            System.out.println("Fase: " + result.getStatus() + " | Premio: "
                    + (result.isPrizePaid() ? "erogato" : "da erogare"));
        } catch (OperationCancelledException ex) {
            System.out.println("Operazione annullata.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Errore proclamazione vincitore: " + ex.getMessage());
        }
    }
}

