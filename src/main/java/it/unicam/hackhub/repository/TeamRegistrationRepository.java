package it.unicam.hackhub.repository;

import it.unicam.hackhub.model.TeamRegistration;

import java.util.List;
import java.util.Optional;

public interface TeamRegistrationRepository {
    Optional<TeamRegistration> findById(long registrationId);

    Optional<TeamRegistration> findByTeamId(long teamId);

    Optional<TeamRegistration> findByTeamIdAndHackathonId(long teamId, long hackathonId);

    List<TeamRegistration> findByHackathonId(long hackathonId);

    TeamRegistration save(TeamRegistration registration);
}
