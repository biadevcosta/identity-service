package com.biadevcosta.identity.infrastructure.bootstrap;

import com.biadevcosta.identity.application.command.RegisterUserCommand;
import com.biadevcosta.identity.application.port.UserRepository;
import com.biadevcosta.identity.application.usecase.RegisterUserUseCase;
import com.biadevcosta.identity.domain.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSeederTest {

    @Mock
    RegisterUserUseCase registerUserUseCase;
    @Mock
    UserRepository userRepository;

    private AdminSeeder seeder() {
        return new AdminSeeder(registerUserUseCase, userRepository, "Admin@Hospital.Local", "pw12345");
    }

    @Test
    void seedsAdmin_whenAbsent() {
        when(userRepository.existsByEmail("admin@hospital.local")).thenReturn(false);

        seeder().run(null);

        ArgumentCaptor<RegisterUserCommand> captor = ArgumentCaptor.forClass(RegisterUserCommand.class);
        verify(registerUserUseCase).execute(captor.capture());
        assertThat(captor.getValue().email()).isEqualTo("Admin@Hospital.Local");
        assertThat(captor.getValue().role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void skips_whenAdminAlreadyExists() {
        when(userRepository.existsByEmail("admin@hospital.local")).thenReturn(true);

        seeder().run(null);

        verifyNoInteractions(registerUserUseCase);
    }
}
