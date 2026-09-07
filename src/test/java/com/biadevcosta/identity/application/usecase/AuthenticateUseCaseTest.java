package com.biadevcosta.identity.application.usecase;

import com.biadevcosta.identity.application.AuthTokens;
import com.biadevcosta.identity.application.port.PasswordHasher;
import com.biadevcosta.identity.application.port.UserRepository;
import com.biadevcosta.identity.domain.Role;
import com.biadevcosta.identity.domain.User;
import com.biadevcosta.identity.domain.exception.InvalidCredentialsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticateUseCaseTest {

    @Mock
    UserRepository userRepository;
    @Mock
    PasswordHasher passwordHasher;
    @Mock
    TokenMinter tokenMinter;

    private AuthenticateUseCase useCase() {
        return new AuthenticateUseCase(userRepository, passwordHasher, tokenMinter);
    }

    private User storedUser() {
        return User.rehydrate("user-1", "me@x.com", "{argon2id}$stored", Role.NURSE, "Me",
                null, null, null, LocalDateTime.now());
    }

    @Test
    void login_validCredentials_normalizesEmail_andReturnsMintedTokens() {
        User user = storedUser();
        AuthTokens expected = new AuthTokens("access", "refresh", 3600);
        when(userRepository.findByEmail("me@x.com")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("pw", "{argon2id}$stored")).thenReturn(true);
        when(tokenMinter.mintFor(user)).thenReturn(expected);

        AuthTokens result = useCase().login("  ME@X.COM ", "pw");

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void login_unknownEmail_failsGenerically_withoutCheckingPasswordOrMinting() {
        when(userRepository.findByEmail("ghost@x.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().login("ghost@x.com", "pw"))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(passwordHasher, tokenMinter);
    }

    @Test
    void login_wrongPassword_failsGenerically_withoutMinting() {
        when(userRepository.findByEmail("me@x.com")).thenReturn(Optional.of(storedUser()));
        when(passwordHasher.matches("wrong", "{argon2id}$stored")).thenReturn(false);

        assertThatThrownBy(() -> useCase().login("me@x.com", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(tokenMinter, never()).mintFor(any());
    }
}
