package com.biadevcosta.identity.application.usecase;

import com.biadevcosta.identity.application.command.RegisterUserCommand;
import com.biadevcosta.identity.application.port.PasswordHasher;
import com.biadevcosta.identity.application.port.UserRepository;
import com.biadevcosta.identity.domain.Role;
import com.biadevcosta.identity.domain.User;
import com.biadevcosta.identity.domain.exception.EmailAlreadyUsedException;
import com.biadevcosta.identity.domain.exception.InvalidUserException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {

    @Mock
    UserRepository userRepository;
    @Mock
    PasswordHasher passwordHasher;

    private RegisterUserUseCase useCase() {
        return new RegisterUserUseCase(userRepository, passwordHasher);
    }

    private RegisterUserCommand command(Role role, String crm, String specialty) {
        return new RegisterUserCommand("New@Hospital.com ", "s3cret", role, "New User", "119", crm, specialty);
    }

    @Test
    void registers_hashesFirst_normalizesEmail_thenChecksUniqueness_thenSaves() {
        when(passwordHasher.hash("s3cret")).thenReturn("{argon2id}$hash");
        when(userRepository.existsByEmail("new@hospital.com")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        User result = useCase().execute(command(Role.NURSE, null, null));

        assertThat(result.email()).isEqualTo("new@hospital.com");
        assertThat(result.passwordHash()).isEqualTo("{argon2id}$hash");
        assertThat(result.role()).isEqualTo(Role.NURSE);

        InOrder inOrder = inOrder(passwordHasher, userRepository);
        inOrder.verify(passwordHasher).hash("s3cret");
        inOrder.verify(userRepository).existsByEmail("new@hospital.com");
        inOrder.verify(userRepository).save(any());
    }

    @Test
    void rejectsDuplicateEmail_withoutSaving() {
        when(passwordHasher.hash(any())).thenReturn("{argon2id}$hash");
        when(userRepository.existsByEmail("new@hospital.com")).thenReturn(true);

        assertThatThrownBy(() -> useCase().execute(command(Role.NURSE, null, null)))
                .isInstanceOf(EmailAlreadyUsedException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsBlankPassword_withoutHashingOrTouchingRepo() {
        RegisterUserCommand blankPassword =
                new RegisterUserCommand("a@b.co", "  ", Role.NURSE, "Nurse", null, null, null);

        assertThatThrownBy(() -> useCase().execute(blankPassword))
                .isInstanceOf(InvalidUserException.class);

        verifyNoInteractions(passwordHasher, userRepository);
    }

    @Test
    void rejectsInvalidDomainData_withoutSaving() {
        when(passwordHasher.hash(any())).thenReturn("{argon2id}$hash");

        // a nurse must not carry crm/specialty
        assertThatThrownBy(() -> useCase().execute(command(Role.NURSE, "CRM-1", "Cardio")))
                .isInstanceOf(InvalidUserException.class);

        verify(userRepository, never()).save(any());
    }
}
