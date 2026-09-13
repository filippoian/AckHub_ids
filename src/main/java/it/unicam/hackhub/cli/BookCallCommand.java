package it.unicam.hackhub.cli;

import it.unicam.hackhub.controller.SupportController;
import it.unicam.hackhub.model.CallBooking;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class BookCallCommand implements Command {
    private final SupportController supportController;
    private final SessionContext sessionContext;
    private final InputHelper inputHelper;

    public BookCallCommand(SupportController supportController,
                           SessionContext sessionContext,
                           InputHelper inputHelper) {
        this.supportController = supportController;
        this.sessionContext = sessionContext;
        this.inputHelper = inputHelper;
    }

    @Override
    public String name() {
        return "book-call";
    }

    @Override
    public void execute() {
        try {
            if (!sessionContext.isUserLoggedIn()) {
                System.out.println("Devi effettuare login USER per prenotare una call.");
                return;
            }

            Optional<Long> currentUserId = sessionContext.getCurrentUserId();
            if (currentUserId.isEmpty()) {
                System.out.println("Sessione utente non valida.");
                return;
            }

            List<SupportController.CallProposalSummary> proposals =
                    supportController.listAvailableCallProposalsForCurrentUser(currentUserId.get());
            Set<Long> selectableProposalIds = new LinkedHashSet<>();
            Map<Long, SupportController.CallProposalSummary> proposalById = new LinkedHashMap<>();
            List<List<String>> rows = new ArrayList<>();
            for (SupportController.CallProposalSummary proposal : proposals) {
                selectableProposalIds.add(proposal.proposalId());
                proposalById.put(proposal.proposalId(), proposal);
                rows.add(List.of(
                        String.valueOf(proposal.proposalId()),
                        String.valueOf(proposal.requestId()),
                        String.valueOf(proposal.proposedStart()),
                        String.valueOf(proposal.proposedEnd()),
                        String.valueOf(proposal.booked())
                ));
            }

            TablePrinter.print(List.of("PROPOSAL_ID", "REQUEST_ID", "START", "END", "BOOKED"), rows);
            if (selectableProposalIds.isEmpty()) {
                return;
            }

            long proposalId = readProposalId(selectableProposalIds);
            CallBooking booking = supportController.bookCall(currentUserId.get(), proposalId);
            SupportController.CallProposalSummary selectedProposal = proposalById.get(proposalId);
            if (selectedProposal == null) {
                throw new IllegalStateException("Proposal non disponibile.");
            }

            List<List<String>> bookingRows = List.of(List.of(
                    String.valueOf(booking.getCallId()),
                    String.valueOf(booking.getProposalId()),
                    String.valueOf(selectedProposal.proposedStart()),
                    String.valueOf(selectedProposal.proposedEnd()),
                    String.valueOf(booking.getCreatedAt()),
                    TablePrinter.truncate(booking.getMeetingLink(), 50)
            ));
            TablePrinter.print(
                    List.of("CALL_ID", "PROPOSAL_ID", "START", "END", "BOOKED_AT", "MEETING_LINK"),
                    bookingRows
            );
        } catch (OperationCancelledException ex) {
            System.out.println("Operazione annullata.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Errore prenotazione call: " + ex.getMessage());
        }
    }

    private long readProposalId(Set<Long> proposalIds) {
        while (true) {
            long proposalId = inputHelper.readLong("Proposal id");
            if (proposalIds.contains(proposalId)) {
                return proposalId;
            }
            System.out.println("Proposal id non valido.");
        }
    }
}
