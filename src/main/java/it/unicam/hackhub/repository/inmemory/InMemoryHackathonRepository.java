package it.unicam.hackhub.repository.inmemory;

import it.unicam.hackhub.model.Hackathon;
import it.unicam.hackhub.repository.HackathonRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryHackathonRepository implements HackathonRepository {
    private final Map<Long, Hackathon> storage = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    @Override
    public Optional<Hackathon> findById(long hackathonId) {
        return Optional.ofNullable(storage.get(hackathonId)).filter(h -> !h.isDeleted());
    }

    @Override
    public Optional<Hackathon> findByName(String hackathonName) {
        if (hackathonName == null) {
            return Optional.empty();
        }
        String normalizedName = hackathonName.trim();
        return storage.values().stream()
                .filter(h -> !h.isDeleted())
                .filter(h -> h.getHackathonName() != null)
                .filter(h -> h.getHackathonName().trim().equalsIgnoreCase(normalizedName))
                .findFirst();
    }

    @Override
    public List<Hackathon> findAll() {
        return storage.values().stream().filter(h -> !h.isDeleted()).toList();
    }

    @Override
    public long count() {
        return findAll().size();
    }

    @Override
    public Hackathon save(Hackathon hackathon) {
        if (hackathon.getHackathonId() <= 0) {
            hackathon.setHackathonId(idGenerator.incrementAndGet());
        }
        storage.put(hackathon.getHackathonId(), hackathon);
        return hackathon;
    }
}
