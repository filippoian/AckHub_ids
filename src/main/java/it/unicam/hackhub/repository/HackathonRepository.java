package it.unicam.hackhub.repository;

import it.unicam.hackhub.model.Hackathon;

import java.util.List;
import java.util.Optional;

public interface HackathonRepository {
    Optional<Hackathon> findById(long hackathonId);

    Optional<Hackathon> findByName(String hackathonName);

    List<Hackathon> findAll();

    long count();

    Hackathon save(Hackathon hackathon);
}
