package it.unicam.hackhub.repository.springdata;

import it.unicam.hackhub.model.User;
import it.unicam.hackhub.repository.UserRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSpringDataRepository extends JpaRepository<User, Long>, UserRepository {
    @Override
    default Optional<User> findById(long id) {
        return findById(Long.valueOf(id));
    }

    Optional<User> findByUserName(String userName);

    List<User> findByTeamId(long teamId);
}

