package it.unicam.hackhub.repository.inmemory;

import it.unicam.hackhub.model.CallBooking;
import it.unicam.hackhub.model.CallProposal;
import it.unicam.hackhub.repository.CallProposalRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryCallProposalRepository implements CallProposalRepository {
    private final Map<Long, CallProposal> storage = new HashMap<>();
    private final Map<Long, CallBooking> bookingByProposalId = new HashMap<>();
    private final AtomicLong proposalIdGenerator = new AtomicLong(0);
    private final AtomicLong bookingIdSeq = new AtomicLong(0);

    @Override
    public List<CallProposal> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public List<CallProposal> findBySupportRequestId(long supportRequestId) {
        List<CallProposal> result = new ArrayList<>();
        for (CallProposal proposal : storage.values()) {
            if (proposal.getRequestId() == supportRequestId) {
                result.add(proposal);
            }
        }
        return result;
    }

    @Override
    public Optional<CallProposal> findById(long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public CallProposal save(CallProposal callProposal) {
        if (callProposal.getProposalId() <= 0) {
            callProposal.setProposalId(proposalIdGenerator.incrementAndGet());
        }
        storage.put(callProposal.getProposalId(), callProposal);
        return callProposal;
    }

    @Override
    public Optional<CallBooking> findBookingByProposalId(long proposalId) {
        return Optional.ofNullable(bookingByProposalId.get(proposalId));
    }

    @Override
    public CallBooking saveBooking(CallBooking booking) {
        if (booking.getCallId() <= 0) {
            booking.setCallId(bookingIdSeq.incrementAndGet());
        }
        bookingByProposalId.put(booking.getProposalId(), booking);
        return booking;
    }
}
