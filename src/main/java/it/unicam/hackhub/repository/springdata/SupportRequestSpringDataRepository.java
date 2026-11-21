package it.unicam.hackhub.repository.springdata;

import it.unicam.hackhub.model.SupportRequest;
import it.unicam.hackhub.repository.SupportRequestRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SupportRequestSpringDataRepository
        extends JpaRepository<SupportRequest, Long>, SupportRequestRepository {
    @Override
    default Optional<SupportRequest> findById(long requestId) {
        return findById(Long.valueOf(requestId));
    }

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from SupportRequest request where request.requestId = :id")
    Optional<SupportRequest> findByIdForUpdate(@Param("id") long requestId);

    List<SupportRequest> findByHackathonId(long hackathonId);
}

