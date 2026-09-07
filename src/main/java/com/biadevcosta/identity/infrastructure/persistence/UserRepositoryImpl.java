package com.biadevcosta.identity.infrastructure.persistence;

import com.biadevcosta.identity.application.port.UserRepository;
import com.biadevcosta.identity.domain.User;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** {@link UserRepository} backed by Spring Data JDBC. */
@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserJdbcRepository jdbc;

    public UserRepositoryImpl(UserJdbcRepository jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public User save(User user) {
        UserEntity entity = jdbc.findById(user.id())
                .map(existing -> {
                    UserMapper.copyInto(user, existing); // keep @Version -> UPDATE
                    return existing;
                })
                .orElseGet(() -> UserMapper.toNewEntity(user)); // version null -> INSERT
        return UserMapper.toDomain(jdbc.save(entity));
    }

    @Override
    public Optional<User> findById(String id) {
        return jdbc.findById(id).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jdbc.findByEmail(email).map(UserMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jdbc.existsByEmail(email);
    }
}
