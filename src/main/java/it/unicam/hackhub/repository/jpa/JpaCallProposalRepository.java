package it.unicam.hackhub.repository.jpa;

import it.unicam.hackhub.model.CallBooking;
import it.unicam.hackhub.model.CallProposal;
import it.unicam.hackhub.repository.CallProposalRepository;
import it.unicam.hackhub.repository.springdata.CallBookingSpringDataRepository;
import it.unicam.hackhub.repository.springdata.CallProposalSpringDataRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaCallProposalRepository implements CallProposalRepository {
    private final CallProposalSpringDataRepository proposalSpringDataRepository;
    private final CallBookingSpringDataRepository bookingSpringDataRepository;

    public JpaCallProposalRepository(CallProposalSpringDataRepository proposalSpringDataRepository,
                                     CallBookingSpringDataRepository bookingSpringDataRepository) {
        this.proposalSpringDataRepository = proposalSpringDataRepository;
        this.bookingSpringDataRepository = bookingSpringDataRepository;
    }

    @Override
    public List<CallProposal> findAll() {
        return proposalSpringDataRepository.findAll();
    }

    @Override
    public List<CallProposal> findBySupportRequestId(long supportRequestId) {
        return proposalSpringDataRepository.findByRequestId(supportRequestId);
    }

    @Override
    public Optional<CallProposal> findById(long id) {
        return proposalSpringDataRepository.findById(id);
    }

    @Override
    public CallProposal save(CallProposal callProposal) {
        return proposalSpringDataRepository.save(callProposal);
    }

    @Override
    public Optional<CallBooking> findBookingByProposalId(long proposalId) {
        return bookingSpringDataRepository.findByProposalId(proposalId);
    }

    @Override
    public CallBooking saveBooking(CallBooking booking) {
        return bookingSpringDataRepository.save(booking);
    }
}

