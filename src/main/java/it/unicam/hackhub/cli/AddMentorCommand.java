package it.unicam.hackhub.cli;

import it.unicam.hackhub.controller.MentorManagementController;
import it.unicam.hackhub.model.enums.StaffRole;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class AddMentorCommand implements Command {
    private final MentorManagementController mentorManagementController;
    private final SessionContext sessionContext;
    private final InputHelper inputHelper;

    public AddMentorCommand(MentorManagementController mentorManagementController,
                            SessionContext sessionContext,
                            InputHelper inputHelper) {
        this.mentorManagementController = mentorManagementController;
        this.sessionContext = sessionContext;
        this.inputHelper = inputHelper;
    }

    @Override
    public String name() {
        return "add-mentor";
    }

    @Override
    public void execute() {
        try {
            if (!sessionContext.isStaffLoggedIn()) {
                System.out.println("Devi effettuare login STAFF per aggiungere mentor.");
                return;
            }
            if (!sessionContext.getStaffRoles().contains(StaffRole.ORGANIZER)) {
                System.out.println("Comando disponibile solo per ORGANIZER.");
                return;
            }

            Optional<Long> currentStaffId = sessionContext.getCurrentStaffId();
            if (currentStaffId.isEmpty()) {
                System.out.println("Sessione staff non valida.");
                return;
            }

            List<MentorManagementController.OrganizerHackathonView> organizerHackathons =
                    mentorManagementController.listOrganizerHackathons(currentStaffId.get());
            if (organizerHackathons.isEmpty()) {
                System.out.println("Non sei organizer di alcun hackathon.");
                return;
            }

            long selectedHackathonId = chooseHackathon(organizerHackathons);
            boolean continueAdding = true;
            while (continueAdding) {
                List<MentorManagementController.MentorCandidateView> candidates =
                        mentorManagementController.listMentorCandidates(currentStaffId.get(), selectedHackathonId);
                if (candidates.isEmpty()) {
                    System.out.println("Nessun candidato mentor disponibile.");
                    return;
                }

                CandidateSelection candidateSelection = toCandidateSelection(candidates);
                TablePrinter.print(List.of("STAFF_ID", "USERNAME", "NAME"), candidateSelection.candidateRows());
                long mentorId = readSingleStaffId("Mentor id", candidateSelection.candidateMentorIds());
                mentorManagementController.addMentor(currentStaffId.get(), selectedHackathonId, mentorId);
                System.out.println("Mentor aggiunto.");

                continueAdding = askContinue();
            }
        } catch (OperationCancelledException ex) {
            System.out.println("Operazione annullata.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Errore aggiunta mentor: " + ex.getMessage());
        }
    }

    private long chooseHackathon(List<MentorManagementController.OrganizerHackathonView> organizerHackathons) {
        Set<Long> organizerHackathonIds = new LinkedHashSet<>();
        List<List<String>> rows = new ArrayList<>();
        for (MentorManagementController.OrganizerHackathonView hackathon : organizerHackathons) {
            organizerHackathonIds.add(hackathon.hackathonId());
            rows.add(List.of(
                    String.valueOf(hackathon.hackathonId()),
                    hackathon.name(),
                    hackathon.status(),
                    hackathon.location()
            ));
        }
        TablePrinter.print(List.of("HACKATHON_ID", "NAME", "STATUS", "LOCATION"), rows);

        while (true) {
            long hackathonId = inputHelper.readLong("Hackathon id");
            if (organizerHackathonIds.contains(hackathonId)) {
                return hackathonId;
            }
            System.out.println("Hackathon non valido.");
        }
    }

    private long readSingleStaffId(String prompt, Set<Long> allowedIds) {
        while (true) {
            long staffId = inputHelper.readLong(prompt);
            if (allowedIds.contains(staffId)) {
                return staffId;
            }
            System.out.println("Mentor id non valido.");
        }
    }

    private boolean askContinue() {
        while (true) {
            String answer = inputHelper.readNonBlank("Aggiungere un altro mentor? (y/n)").trim();
            if ("y".equalsIgnoreCase(answer)) {
                return true;
            }
            if ("n".equalsIgnoreCase(answer)) {
                return false;
            }
            System.out.println("Risposta non valida. Inserisci y o n.");
        }
    }

    private CandidateSelection toCandidateSelection(List<MentorManagementController.MentorCandidateView> candidates) {
        Set<Long> candidateMentorIds = new LinkedHashSet<>();
        List<List<String>> candidateRows = new ArrayList<>();
        for (MentorManagementController.MentorCandidateView candidate : candidates) {
            candidateMentorIds.add(candidate.staffId());
            candidateRows.add(List.of(
                    String.valueOf(candidate.staffId()),
                    candidate.username(),
                    candidate.name()
            ));
        }
        return new CandidateSelection(candidateMentorIds, candidateRows);
    }

    private record CandidateSelection(Set<Long> candidateMentorIds, List<List<String>> candidateRows) {
    }
}
