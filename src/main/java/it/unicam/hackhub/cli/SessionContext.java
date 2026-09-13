package it.unicam.hackhub.cli;

import it.unicam.hackhub.model.enums.StaffRole;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

public class SessionContext implements it.unicam.hackhub.security.SessionInvalidator {
    private Long currentUserId;
    private Long currentStaffId;
    private int userProfileNotFoundCount;
    private final Set<StaffRole> staffRoles = EnumSet.noneOf(StaffRole.class);

    @Override
    public void invalidateUser(long userId) {
        if (currentUserId != null && currentUserId == userId) logout();
    }

    public boolean isUserLoggedIn() {
        return currentUserId != null;
    }

    public boolean isStaffLoggedIn() {
        return currentStaffId != null;
    }

    public void loginUser(long userId) {
        this.currentUserId = userId;
        this.currentStaffId = null;
        this.staffRoles.clear();
    }

    public void loginStaff(long staffId) {
        this.currentStaffId = staffId;
        this.currentUserId = null;
        this.staffRoles.clear();
    }

    public void logout() {
        this.currentUserId = null;
        this.currentStaffId = null;
        this.staffRoles.clear();
    }

    public int getUserProfileNotFoundCount() {
        return userProfileNotFoundCount;
    }

    public void incrementUserProfileNotFoundCount() {
        userProfileNotFoundCount++;
    }

    public void resetUserProfileNotFoundCount() {
        userProfileNotFoundCount = 0;
    }

    public Optional<Long> getCurrentUserId() {
        return Optional.ofNullable(currentUserId);
    }

    public Optional<Long> getCurrentStaffId() {
        return Optional.ofNullable(currentStaffId);
    }

    public Set<StaffRole> getStaffRoles() {
        return Set.copyOf(staffRoles);
    }

    public void setStaffRoles(Set<StaffRole> roles) {
        staffRoles.clear();
        if (roles != null) {
            staffRoles.addAll(roles);
        }
    }
}
