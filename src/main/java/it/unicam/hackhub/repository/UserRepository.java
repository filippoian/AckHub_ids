package it.unicam.hackhub.repository;

import it.unicam.hackhub.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(long id);
    Optional<User> findByUserName(String userName);
    List<User> findByTeamId(long teamId);
    List<User> findAll();
    User save(User user);
}
