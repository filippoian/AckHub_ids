package it.unicam.hackhub.repository;

import it.unicam.hackhub.model.Team;

import java.util.Optional;

public interface TeamRepository {
    Optional<Team> findById(long id);
    Optional<Team> findByName(String name);
    Team save(Team team);
}
