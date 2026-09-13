package it.unicam.hackhub.repository;

import it.unicam.hackhub.model.SupportRequest;

import java.util.List;
import java.util.Optional;

public interface SupportRequestRepository {
    Optional<SupportRequest> findById(long requestId);
    List<SupportRequest> findAll();
    List<SupportRequest> findByHackathonId(long hackathonId);
    SupportRequest save(SupportRequest supportRequest);
}
