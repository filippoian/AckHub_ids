package it.unicam.hackhub.repository.springdata;

import it.unicam.hackhub.model.CallBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CallBookingSpringDataRepository extends JpaRepository<CallBooking, Long> {
    Optional<CallBooking> findByProposalId(long proposalId);
}

