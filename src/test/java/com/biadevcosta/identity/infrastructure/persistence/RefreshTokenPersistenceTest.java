package com.biadevcosta.identity.infrastructure.persistence;

import com.biadevcosta.identity.domain.RefreshToken;
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
class RefreshTokenPersistenceTest {

    @Mock
    RefreshTokenJdbcRepository jdbc;

    private static RefreshToken sample(String id) {
        return RefreshToken.rehydrate(id, "user-1", "sha-1",
                LocalDateTime.of(2026, 1, 8, 12, 0), false, LocalDateTime.of(2026, 1, 1, 12, 0));
    }

    @Test
    void mapper_roundTripsThroughEntity() {
        RefreshToken original = sample("rt-1");

        RefreshTokenEntity entity = RefreshTokenMapper.toNewEntity(original);
        RefreshToken back = RefreshTokenMapper.toDomain(entity);

        assertThat(entity.getVersion()).isNull();
        assertThat(entity.isRevoked()).isFalse();
        assertThat(back.id()).isEqualTo("rt-1");
        assertThat(back.userId()).isEqualTo("user-1");
        assertThat(back.tokenHash()).isEqualTo("sha-1");
        assertThat(back.expiresAt()).isEqualTo(original.expiresAt());
    }

    @Test
    void save_newToken_insertsWithNullVersion() {
        when(jdbc.findById("rt-1")).thenReturn(Optional.empty());
        when(jdbc.save(any())).thenAnswer(i -> i.getArgument(0));
        RefreshTokenRepositoryImpl repository = new RefreshTokenRepositoryImpl(jdbc);

        repository.save(sample("rt-1"));

        ArgumentCaptor<RefreshTokenEntity> captor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
        verify(jdbc).save(captor.capture());
        assertThat(captor.getValue().getVersion()).isNull();
    }

    @Test
    void save_revokedToken_keepsVersionAndFlipsRevoked() {
        RefreshTokenEntity existing = RefreshTokenMapper.toNewEntity(sample("rt-1"));
        existing.setVersion(2L);
        when(jdbc.findById("rt-1")).thenReturn(Optional.of(existing));
        when(jdbc.save(any())).thenAnswer(i -> i.getArgument(0));
        RefreshTokenRepositoryImpl repository = new RefreshTokenRepositoryImpl(jdbc);

        RefreshToken revoked = sample("rt-1");
        revoked.revoke();
        repository.save(revoked);

        ArgumentCaptor<RefreshTokenEntity> captor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
        verify(jdbc).save(captor.capture());
        assertThat(captor.getValue().getVersion()).isEqualTo(2L);
        assertThat(captor.getValue().isRevoked()).isTrue();
    }

    @Test
    void findByTokenHash_mapsEntityToDomain() {
        when(jdbc.findByTokenHash("sha-1"))
                .thenReturn(Optional.of(RefreshTokenMapper.toNewEntity(sample("rt-1"))));
        RefreshTokenRepositoryImpl repository = new RefreshTokenRepositoryImpl(jdbc);

        Optional<RefreshToken> found = repository.findByTokenHash("sha-1");

        assertThat(found).isPresent();
        assertThat(found.get().userId()).isEqualTo("user-1");
    }
}
