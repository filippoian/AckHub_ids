package it.unicam.hackhub.cli;

import it.unicam.hackhub.controller.HackathonController;
import it.unicam.hackhub.model.Hackathon;

import java.util.ArrayList;
import java.util.List;

public class ListHackathonsCommand implements Command {
    private final HackathonController hackathonController;

    public ListHackathonsCommand(HackathonController hackathonController) {
        this.hackathonController = hackathonController;
    }

    @Override
    public String name() {
        return "list-hackathons";
    }

    @Override
    public void execute() {
        try {
            List<Hackathon> hackathons = hackathonController.listHackathons();

            List<List<String>> rows = new ArrayList<>();
            for (Hackathon hackathon : hackathons) {
                rows.add(List.of(
                        String.valueOf(hackathon.getHackathonId()),
                        nullToDash(hackathon.getHackathonName()),
                        String.valueOf(hackathon.getStatus()),
                        nullToDash(hackathon.getLocation())
                ));
            }

            TablePrinter.print(List.of("ID", "NAME", "STATUS", "LOCATION"), rows);
        } catch (OperationCancelledException ex) {
            System.out.println("Operazione annullata.");
        }
    }

    private String nullToDash(String value) {
        return value == null ? "-" : value;
    }
}
