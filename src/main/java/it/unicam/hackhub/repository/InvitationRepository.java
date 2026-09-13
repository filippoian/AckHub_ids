package it.unicam.hackhub.repository;

import it.unicam.hackhub.model.Invitation;

import java.util.List;
import java.util.Optional;

public interface InvitationRepository {
    Optional<Invitation> findById(long invitationId);

    List<Invitation> findByInvitedUserId(long invitedUserId);

    Optional<Invitation> findPendingByTeamAndUser(long teamId, long userId);

    List<Invitation> findByTeamId(long teamId);

    Invitation save(Invitation invitation);
}
