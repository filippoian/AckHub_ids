package it.unicam.hackhub.repository.springdata;

import it.unicam.hackhub.model.CallProposal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CallProposalSpringDataRepository extends JpaRepository<CallProposal, Long> {
    List<CallProposal> findByRequestId(long requestId);
}

