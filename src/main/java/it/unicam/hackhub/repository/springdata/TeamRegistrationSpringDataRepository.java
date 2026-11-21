package it.unicam.hackhub.repository.springdata;

import it.unicam.hackhub.model.TeamRegistration;
import it.unicam.hackhub.repository.TeamRegistrationRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamRegistrationSpringDataRepository
        extends JpaRepository<TeamRegistration, Long>, TeamRegistrationRepository {
    @Override
    default Optional<TeamRegistration> findById(long registrationId) {
        return findById(Long.valueOf(registrationId));
    }

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select registration from TeamRegistration registration where registration.registrationId = :id")
    Optional<TeamRegistration> findByIdForUpdate(@Param("id") long registrationId);

    Optional<TeamRegistration> findByTeamId(long teamId);

    Optional<TeamRegistration> findByTeamIdAndHackathonId(long teamId, long hackathonId);

    List<TeamRegistration> findByHackathonId(long hackathonId);
}

