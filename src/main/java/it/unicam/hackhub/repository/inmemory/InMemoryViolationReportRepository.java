package it.unicam.hackhub.repository.inmemory;

import it.unicam.hackhub.model.ViolationReport;
import it.unicam.hackhub.repository.ViolationReportRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryViolationReportRepository implements ViolationReportRepository {
    private final Map<Long, ViolationReport> byId = new TreeMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    @Override
    public ViolationReport save(ViolationReport violationReport) {
        if (violationReport.getReportId() <= 0) {
            violationReport.setReportId(idGenerator.incrementAndGet());
        }
        byId.put(violationReport.getReportId(), violationReport);
        return violationReport;
    }

    @Override
    public Optional<ViolationReport> findById(long reportId) {
        return Optional.ofNullable(byId.get(reportId));
    }

    @Override
    public List<ViolationReport> findByHackathonId(long hackathonId) {
        List<ViolationReport> result = new ArrayList<>();
        for (ViolationReport report : byId.values()) {
            if (report.getHackathonId() == hackathonId) {
                result.add(report);
            }
        }
        return result;
    }

    @Override
    public List<ViolationReport> findAll() {
        return new ArrayList<>(byId.values());
    }

    @Override
    public List<ViolationReport> findPendingByHackathonId(long hackathonId) {
        List<ViolationReport> result = new ArrayList<>();
        for (ViolationReport report : byId.values()) {
            if (report.getHackathonId() == hackathonId && report.getDecision() == null) {
                result.add(report);
            }
        }
        return result;
    }
}
