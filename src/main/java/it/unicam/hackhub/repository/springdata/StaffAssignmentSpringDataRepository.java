package it.unicam.hackhub.repository.springdata;

import it.unicam.hackhub.model.StaffAssignment;
import it.unicam.hackhub.model.enums.StaffRole;
import it.unicam.hackhub.repository.StaffAssignmentRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffAssignmentSpringDataRepository
        extends JpaRepository<StaffAssignment, Long>, StaffAssignmentRepository {
    List<StaffAssignment> findByStaffId(long staffId);

    List<StaffAssignment> findByHackathonId(long hackathonId);

    List<StaffAssignment> findByHackathonIdAndRole(long hackathonId, StaffRole role);
}

