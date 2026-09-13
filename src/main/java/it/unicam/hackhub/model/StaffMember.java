package it.unicam.hackhub.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "staff_members")
public class StaffMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long staffId;
    private String staffName;
    @Column(unique = true)
    private String staffUsername;
    private String passwordHash;

    public StaffMember() {
    }

    public StaffMember(long staffId, String staffName, String staffUsername, String passwordHash) {
        this.staffId = staffId;
        this.staffName = staffName;
        this.staffUsername = staffUsername;
        this.passwordHash = passwordHash;
    }

    public long getStaffId() {
        return staffId;
    }

    public void setStaffId(long staffId) {
        this.staffId = staffId;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public String getStaffUsername() {
        return staffUsername;
    }

    public void setStaffUsername(String staffUsername) {
        this.staffUsername = staffUsername;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof StaffMember other)) {
            return false;
        }
        return staffId == other.staffId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(staffId);
    }
}
