package it.unicam.hackhub.repository.springdata;

import it.unicam.hackhub.model.Hackathon;
import it.unicam.hackhub.repository.HackathonRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface HackathonSpringDataRepository extends JpaRepository<Hackathon, Long>, HackathonRepository {
    @Override
    default Optional<Hackathon> findById(long hackathonId) {
        return findById(Long.valueOf(hackathonId));
    }

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select event from Hackathon event where event.hackathonId = :id")
    Optional<Hackathon> findByIdForUpdate(@Param("id") long hackathonId);

    Optional<Hackathon> findByHackathonName(String hackathonName);

    @Override
    default Optional<Hackathon> findByName(String hackathonName) {
        return findByHackathonName(hackathonName);
    }
}

