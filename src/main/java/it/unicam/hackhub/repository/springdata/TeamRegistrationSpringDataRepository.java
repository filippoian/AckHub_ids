package it.unicam.hackhub.repository.springdata;

import it.unicam.hackhub.model.TeamRegistration;
import it.unicam.hackhub.repository.TeamRegistrationRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRegistrationSpringDataRepository
        extends JpaRepository<TeamRegistration, Long>, TeamRegistrationRepository {
    @Override
    default Optional<TeamRegistration> findById(long registrationId) {
        return findById(Long.valueOf(registrationId));
    }

    Optional<TeamRegistration> findByTeamId(long teamId);

    Optional<TeamRegistration> findByTeamIdAndHackathonId(long teamId, long hackathonId);

    List<TeamRegistration> findByHackathonId(long hackathonId);
}

