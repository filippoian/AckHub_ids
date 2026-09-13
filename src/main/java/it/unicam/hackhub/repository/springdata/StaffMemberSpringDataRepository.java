package it.unicam.hackhub.repository.springdata;

import it.unicam.hackhub.model.StaffMember;
import it.unicam.hackhub.repository.StaffMemberRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StaffMemberSpringDataRepository
        extends JpaRepository<StaffMember, Long>, StaffMemberRepository {
    @Override
    default Optional<StaffMember> findById(long staffId) {
        return findById(Long.valueOf(staffId));
    }

    Optional<StaffMember> findByStaffUsername(String staffUsername);

    @Override
    default Optional<StaffMember> findByUsername(String username) {
        return findByStaffUsername(username);
    }
}

