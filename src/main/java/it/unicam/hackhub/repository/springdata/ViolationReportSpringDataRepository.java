package it.unicam.hackhub.repository.springdata;

import it.unicam.hackhub.model.ViolationReport;
import it.unicam.hackhub.repository.ViolationReportRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ViolationReportSpringDataRepository
        extends JpaRepository<ViolationReport, Long>, ViolationReportRepository {
    @Override
    default Optional<ViolationReport> findById(long reportId) {
        return findById(Long.valueOf(reportId));
    }

    List<ViolationReport> findByHackathonId(long hackathonId);

    List<ViolationReport> findByHackathonIdAndDecisionIsNull(long hackathonId);

    @Override
    default List<ViolationReport> findPendingByHackathonId(long hackathonId) {
        return findByHackathonIdAndDecisionIsNull(hackathonId);
    }
}

