package it.unicam.hackhub.repository;

import it.unicam.hackhub.model.StaffMember;

import java.util.List;
import java.util.Optional;

public interface StaffMemberRepository {
    Optional<StaffMember> findById(long staffId);

    List<StaffMember> findAll();

    Optional<StaffMember> findByUsername(String username);

    StaffMember save(StaffMember staffMember);
}
