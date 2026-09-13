package it.unicam.hackhub.repository;

import it.unicam.hackhub.model.StaffAssignment;
import it.unicam.hackhub.model.enums.StaffRole;

import java.util.List;

public interface StaffAssignmentRepository {
    List<StaffAssignment> findByStaffId(long staffId);

    List<StaffAssignment> findByHackathonId(long hackathonId);

    List<StaffAssignment> findByHackathonIdAndRole(long hackathonId, StaffRole role);

    StaffAssignment save(StaffAssignment staffAssignment);
}
