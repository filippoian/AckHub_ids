package it.unicam.hackhub.repository.springdata;

import it.unicam.hackhub.model.Hackathon;
import it.unicam.hackhub.repository.HackathonRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HackathonSpringDataRepository extends JpaRepository<Hackathon, Long>, HackathonRepository {
    @Override
    default Optional<Hackathon> findById(long hackathonId) {
        return findById(Long.valueOf(hackathonId));
    }

    Optional<Hackathon> findByHackathonName(String hackathonName);

    @Override
    default Optional<Hackathon> findByName(String hackathonName) {
        return findByHackathonName(hackathonName);
    }
}

