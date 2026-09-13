package it.unicam.hackhub.repository.inmemory;

import it.unicam.hackhub.model.User;
import it.unicam.hackhub.repository.UserRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryUserRepository implements UserRepository {
    private final Map<Long, User> storage = new HashMap<>();
    private final Map<String, Long> userNameIndex = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    @Override
    public Optional<User> findById(long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<User> findByUserName(String userName) {
        Long id = userNameIndex.get(userName);
        return id == null ? Optional.empty() : Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<User> findByTeamId(long teamId) {
        List<User> result = new ArrayList<>();
        for (User user : storage.values()) {
            if (user.getTeamId() != null && user.getTeamId() == teamId) {
                result.add(user);
            }
        }
        return result;
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public User save(User user) {
        if (user.getUserId() <= 0) {
            user.setUserId(idGenerator.incrementAndGet());
        }
        userNameIndex.entrySet().removeIf(e -> e.getValue() == user.getUserId()
                && !e.getKey().equals(user.getUserName()));
        storage.put(user.getUserId(), user);
        if (user.getUserName() != null) {
            userNameIndex.put(user.getUserName(), user.getUserId());
        }
        return user;
    }
}
