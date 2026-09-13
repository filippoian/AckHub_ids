package it.unicam.hackhub.model;

import it.unicam.hackhub.model.enums.StaffRole;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "staff_assignments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"staffId", "hackathonId", "role"})
)
public class StaffAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long assignmentId;
    private long staffId;
    private long hackathonId;
    @Enumerated(EnumType.STRING)
    private StaffRole role;

    public StaffAssignment() {
    }

    public StaffAssignment(long assignmentId, long staffId, long hackathonId, StaffRole role) {
        this.assignmentId = assignmentId;
        this.staffId = staffId;
        this.hackathonId = hackathonId;
        this.role = role;
    }

    public long getAssignmentId() { return assignmentId; }
    public void setAssignmentId(long assignmentId) { this.assignmentId = assignmentId; }
    public long getStaffId() { return staffId; }
    public void setStaffId(long staffId) { this.staffId = staffId; }
    public long getHackathonId() { return hackathonId; }
    public void setHackathonId(long hackathonId) { this.hackathonId = hackathonId; }
    public StaffRole getRole() { return role; }
    public void setRole(StaffRole role) { this.role = role; }
}
