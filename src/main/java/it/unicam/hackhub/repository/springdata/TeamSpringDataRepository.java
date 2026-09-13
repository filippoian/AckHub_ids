package it.unicam.hackhub.repository.springdata;

import it.unicam.hackhub.model.Team;
import it.unicam.hackhub.repository.TeamRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamSpringDataRepository extends JpaRepository<Team, Long>, TeamRepository {
    Optional<Team> findByTeamName(String teamName);

    @Override
    default Optional<Team> findById(long id) {
        return findById(Long.valueOf(id));
    }

    @Override
    default Optional<Team> findByName(String name) {
        return findByTeamName(name);
    }
}

