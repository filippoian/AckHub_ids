package it.unicam.hackhub.repository.inmemory;

import it.unicam.hackhub.model.TeamRegistration;
import it.unicam.hackhub.repository.TeamRegistrationRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryTeamRegistrationRepository implements TeamRegistrationRepository {
    private final Map<Long, TeamRegistration> byId = new HashMap<>();
    private final Map<Long, Long> teamToRegistrationId = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    @Override
    public Optional<TeamRegistration> findById(long registrationId) {
        return Optional.ofNullable(byId.get(registrationId));
    }

    @Override
    public Optional<TeamRegistration> findByTeamId(long teamId) {
        Long registrationId = teamToRegistrationId.get(teamId);
        if (registrationId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byId.get(registrationId));
    }

    @Override
    public Optional<TeamRegistration> findByTeamIdAndHackathonId(long teamId, long hackathonId) {
        Optional<TeamRegistration> registrationOpt = findByTeamId(teamId);
        if (registrationOpt.isEmpty()) {
            return Optional.empty();
        }
        TeamRegistration registration = registrationOpt.get();
        if (registration.getHackathonId() != hackathonId) {
            return Optional.empty();
        }
        return Optional.of(registration);
    }

    @Override
    public List<TeamRegistration> findByHackathonId(long hackathonId) {
        List<TeamRegistration> result = new ArrayList<>();
        for (TeamRegistration registration : byId.values()) {
            if (registration.getHackathonId() == hackathonId) {
                result.add(registration);
            }
        }
        return result;
    }

    @Override
    public TeamRegistration save(TeamRegistration registration) {
        long previousTeamId = -1;
        if (registration.getRegistrationId() > 0) {
            TeamRegistration existing = byId.get(registration.getRegistrationId());
            if (existing != null) {
                previousTeamId = existing.getTeamId();
            }
        }

        if (registration.getRegistrationId() <= 0) {
            registration.setRegistrationId(idGenerator.incrementAndGet());
        }

        Long existingRegistrationForTeam = teamToRegistrationId.get(registration.getTeamId());
        if (existingRegistrationForTeam != null && existingRegistrationForTeam != registration.getRegistrationId()) {
            throw new IllegalStateException("Team already registered (invariant violation)");
        }
        if (previousTeamId != -1 && previousTeamId != registration.getTeamId()) {
            teamToRegistrationId.remove(previousTeamId);
        }

        byId.put(registration.getRegistrationId(), registration);
        teamToRegistrationId.put(registration.getTeamId(), registration.getRegistrationId());
        return registration;
    }
}
