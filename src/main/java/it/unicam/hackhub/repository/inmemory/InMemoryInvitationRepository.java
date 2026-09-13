package it.unicam.hackhub.repository.inmemory;

import it.unicam.hackhub.model.Invitation;
import it.unicam.hackhub.model.enums.InvitationStatus;
import it.unicam.hackhub.repository.InvitationRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryInvitationRepository implements InvitationRepository {
    private final Map<Long, Invitation> storage = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    @Override
    public Optional<Invitation> findById(long invitationId) {
        return Optional.ofNullable(storage.get(invitationId));
    }

    @Override
    public List<Invitation> findByInvitedUserId(long invitedUserId) {
        List<Invitation> result = new ArrayList<>();
        for (Invitation invitation : storage.values()) {
            if (invitation.getInvitedUserId() == invitedUserId) {
                result.add(invitation);
            }
        }
        return result;
    }

    @Override
    public Optional<Invitation> findPendingByTeamAndUser(long teamId, long userId) {
        for (Invitation invitation : storage.values()) {
            if (invitation.getTeamId() == teamId && invitation.getInvitedUserId() == userId && invitation.getStatus() == InvitationStatus.PENDING) {
                return Optional.of(invitation);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Invitation> findByTeamId(long teamId) {
        return storage.values().stream().filter(i -> i.getTeamId() == teamId).toList();
    }

    @Override
    public Invitation save(Invitation invitation) {
        if (invitation.getInvitationId() <= 0) {
            invitation.setInvitationId(idGenerator.incrementAndGet());
        }

        storage.put(invitation.getInvitationId(), invitation);
        return invitation;
    }
}
