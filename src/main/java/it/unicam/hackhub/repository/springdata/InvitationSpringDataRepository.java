package it.unicam.hackhub.repository.springdata;

import it.unicam.hackhub.model.Invitation;
import it.unicam.hackhub.model.enums.InvitationStatus;
import it.unicam.hackhub.repository.InvitationRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvitationSpringDataRepository extends JpaRepository<Invitation, Long>, InvitationRepository {
    @Override
    default Optional<Invitation> findById(long invitationId) {
        return findById(Long.valueOf(invitationId));
    }

    List<Invitation> findByInvitedUserId(long invitedUserId);

    Optional<Invitation> findByTeamIdAndInvitedUserIdAndStatus(
            long teamId,
            long invitedUserId,
            InvitationStatus status
    );

    @Override
    default Optional<Invitation> findPendingByTeamAndUser(long teamId, long userId) {
        return findByTeamIdAndInvitedUserIdAndStatus(teamId, userId, InvitationStatus.PENDING);
    }
}

