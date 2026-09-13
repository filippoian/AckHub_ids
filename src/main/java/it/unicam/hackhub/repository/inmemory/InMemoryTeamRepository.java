package it.unicam.hackhub.repository.inmemory;

import it.unicam.hackhub.model.Team;
import it.unicam.hackhub.repository.TeamRepository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryTeamRepository implements TeamRepository {
    private final Map<Long, Team> storage = new LinkedHashMap<>();
    private final Map<String, Long> nameIndex = new LinkedHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    @Override
    public Optional<Team> findById(long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<Team> findByName(String name) {
        Long id = nameIndex.get(name);
        return id == null ? Optional.empty() : Optional.ofNullable(storage.get(id));
    }

    @Override
    public Team save(Team team) {
        if (team.getTeamId() <= 0) {
            team.setTeamId(idGenerator.incrementAndGet());
        }
        storage.put(team.getTeamId(), team);
        if (team.getTeamName() != null) {
            nameIndex.put(team.getTeamName(), team.getTeamId());
        }
        return team;
    }
}
