package it.unicam.hackhub.repository.inmemory;

import it.unicam.hackhub.model.StaffMember;
import it.unicam.hackhub.repository.StaffMemberRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryStaffMemberRepository implements StaffMemberRepository {
    private final Map<Long, StaffMember> storage = new HashMap<>();
    private final Map<String, Long> usernameIndex = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    @Override
    public Optional<StaffMember> findById(long staffId) {
        return Optional.ofNullable(storage.get(staffId));
    }

    @Override
    public List<StaffMember> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public Optional<StaffMember> findByUsername(String username) {
        Long id = usernameIndex.get(username);
        return id == null ? Optional.empty() : Optional.ofNullable(storage.get(id));
    }

    @Override
    public StaffMember save(StaffMember staffMember) {
        if (staffMember.getStaffId() <= 0) {
            staffMember.setStaffId(idGenerator.incrementAndGet());
        }
        storage.put(staffMember.getStaffId(), staffMember);
        if (staffMember.getStaffUsername() != null) {
            usernameIndex.put(staffMember.getStaffUsername(), staffMember.getStaffId());
        }
        return staffMember;
    }
}
