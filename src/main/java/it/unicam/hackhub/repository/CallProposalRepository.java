package it.unicam.hackhub.repository;

import it.unicam.hackhub.model.CallBooking;
import it.unicam.hackhub.model.CallProposal;

import java.util.List;
import java.util.Optional;

public interface CallProposalRepository {
    List<CallProposal> findAll();
    List<CallProposal> findBySupportRequestId(long supportRequestId);
    Optional<CallProposal> findById(long id);
    CallProposal save(CallProposal callProposal);
    Optional<CallBooking> findBookingByProposalId(long proposalId);
    CallBooking saveBooking(CallBooking booking);
}
