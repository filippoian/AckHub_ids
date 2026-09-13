package it.unicam.hackhub.repository.springdata;

import it.unicam.hackhub.model.SupportRequest;
import it.unicam.hackhub.repository.SupportRequestRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupportRequestSpringDataRepository
        extends JpaRepository<SupportRequest, Long>, SupportRequestRepository {
    @Override
    default Optional<SupportRequest> findById(long requestId) {
        return findById(Long.valueOf(requestId));
    }

    List<SupportRequest> findByHackathonId(long hackathonId);
}

