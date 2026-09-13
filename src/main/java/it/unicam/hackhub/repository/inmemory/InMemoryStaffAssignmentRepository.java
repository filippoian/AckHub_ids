package it.unicam.hackhub.repository.inmemory;

import it.unicam.hackhub.model.StaffAssignment;
import it.unicam.hackhub.model.enums.StaffRole;
import it.unicam.hackhub.repository.StaffAssignmentRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class InMemoryStaffAssignmentRepository implements StaffAssignmentRepository {
    private final Map<Long, StaffAssignment> storage = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    @Override
    public List<StaffAssignment> findByStaffId(long staffId) {
        return storage.values().stream()
                .filter(assignment -> assignment.getStaffId() == staffId)
                .collect(Collectors.toList());
    }

    @Override
    public List<StaffAssignment> findByHackathonId(long hackathonId) {
        return storage.values().stream()
                .filter(assignment -> assignment.getHackathonId() == hackathonId)
                .collect(Collectors.toList());
    }

    @Override
    public List<StaffAssignment> findByHackathonIdAndRole(long hackathonId, StaffRole role) {
        return storage.values().stream()
                .filter(assignment -> assignment.getHackathonId() == hackathonId)
                .filter(assignment -> assignment.getRole() == role)
                .collect(Collectors.toList());
    }

    @Override
    public StaffAssignment save(StaffAssignment staffAssignment) {
        if (staffAssignment.getAssignmentId() <= 0) {
            staffAssignment.setAssignmentId(idGenerator.incrementAndGet());
        }
        storage.put(staffAssignment.getAssignmentId(), staffAssignment);
        return staffAssignment;
    }
}
