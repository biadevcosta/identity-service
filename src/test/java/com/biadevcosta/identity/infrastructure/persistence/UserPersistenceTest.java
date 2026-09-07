package com.biadevcosta.identity.infrastructure.persistence;

import com.biadevcosta.identity.domain.Role;
import com.biadevcosta.identity.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPersistenceTest {

    @Mock
    UserJdbcRepository jdbc;

    private static User sample(String id) {
        return User.rehydrate(id, "doc@x.com", "{argon2id}$h", Role.DOCTOR, "Dr X",
                "119", "CRM-1", "Cardio", LocalDateTime.of(2026, 1, 1, 9, 0));
    }

    @Test
    void mapper_roundTripsThroughEntity() {
        User original = sample("user-1");

        UserEntity entity = UserMapper.toNewEntity(original);
        User back = UserMapper.toDomain(entity);

        assertThat(entity.getRole()).isEqualTo("DOCTOR");
        assertThat(entity.getVersion()).isNull();
        assertThat(back.id()).isEqualTo("user-1");
        assertThat(back.email()).isEqualTo("doc@x.com");
        assertThat(back.role()).isEqualTo(Role.DOCTOR);
        assertThat(back.crm()).isEqualTo("CRM-1");
        assertThat(back.createdAt()).isEqualTo(original.createdAt());
    }

    @Test
    void save_newUser_insertsWithNullVersion() {
        when(jdbc.findById("user-1")).thenReturn(Optional.empty());
        when(jdbc.save(any())).thenAnswer(i -> i.getArgument(0));
        UserRepositoryImpl repository = new UserRepositoryImpl(jdbc);

        repository.save(sample("user-1"));

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(jdbc).save(captor.capture());
        assertThat(captor.getValue().getVersion()).isNull();
        assertThat(captor.getValue().getId()).isEqualTo("user-1");
    }

    @Test
    void save_existingUser_keepsVersionForUpdate() {
        UserEntity existing = UserMapper.toNewEntity(sample("user-1"));
        existing.setVersion(4L);
        when(jdbc.findById("user-1")).thenReturn(Optional.of(existing));
        when(jdbc.save(any())).thenAnswer(i -> i.getArgument(0));
        UserRepositoryImpl repository = new UserRepositoryImpl(jdbc);

        repository.save(sample("user-1"));

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(jdbc).save(captor.capture());
        assertThat(captor.getValue().getVersion()).isEqualTo(4L);
    }

    @Test
    void findByEmail_mapsEntityToDomain() {
        when(jdbc.findByEmail("doc@x.com"))
                .thenReturn(Optional.of(UserMapper.toNewEntity(sample("user-1"))));
        UserRepositoryImpl repository = new UserRepositoryImpl(jdbc);

        Optional<User> found = repository.findByEmail("doc@x.com");

        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo("user-1");
    }

    @Test
    void existsByEmail_delegatesToJdbc() {
        when(jdbc.existsByEmail("doc@x.com")).thenReturn(true);
        UserRepositoryImpl repository = new UserRepositoryImpl(jdbc);

        assertThat(repository.existsByEmail("doc@x.com")).isTrue();
    }
}
