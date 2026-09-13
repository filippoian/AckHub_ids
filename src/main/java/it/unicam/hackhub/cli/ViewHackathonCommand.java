package it.unicam.hackhub.cli;

import it.unicam.hackhub.controller.HackathonController;
import it.unicam.hackhub.model.Hackathon;

import java.util.Optional;

public class ViewHackathonCommand implements Command {
    private final HackathonController hackathonController;
    private final InputHelper inputHelper;

    public ViewHackathonCommand(HackathonController hackathonController, InputHelper inputHelper) {
        this.hackathonController = hackathonController;
        this.inputHelper = inputHelper;
    }

    @Override
    public String name() {
        return "view-hackathon";
    }

    @Override
    public void execute() {
        try {
            long hackathonId = inputHelper.readLong("Hackathon id");
            Optional<Hackathon> hackathonOpt = hackathonController.getHackathonDetails(hackathonId);
            if (hackathonOpt.isEmpty()) {
                System.out.println("Hackathon non trovato.");
                return;
            }

            Hackathon hackathon = hackathonOpt.get();
            System.out.println("id: " + hackathon.getHackathonId());
            System.out.println("name: " + hackathon.getHackathonName());
            System.out.println("regulation: " + hackathon.getRegulation());
            System.out.println("registrationDeadline: " + hackathon.getRegistrationDeadline());
            System.out.println("startDate: " + hackathon.getStartDate());
            System.out.println("endDate: " + hackathon.getEndDate());
            System.out.println("submissionDeadline: " + hackathon.getSubmissionDeadline());
            System.out.println("location: " + hackathon.getLocation());
            System.out.println("prizeAmount: " + hackathon.getPrizeAmount());
            System.out.println("maxTeamSize: " + hackathon.getMaxTeamSize());
            System.out.println("status: " + hackathon.getStatus());
            System.out.println("winnerTeamId: " + (hackathon.getWinnerTeamId() == null ? "N/A" : hackathon.getWinnerTeamId()));
        } catch (OperationCancelledException ex) {
            System.out.println("Operazione annullata.");
        }
    }
}
