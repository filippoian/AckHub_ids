package it.unicam.hackhub.repository;

import it.unicam.hackhub.model.ViolationReport;

import java.util.List;
import java.util.Optional;

public interface ViolationReportRepository {
    ViolationReport save(ViolationReport violationReport);
    Optional<ViolationReport> findById(long reportId);
    List<ViolationReport> findByHackathonId(long hackathonId);
    List<ViolationReport> findAll();
    List<ViolationReport> findPendingByHackathonId(long hackathonId);
}
