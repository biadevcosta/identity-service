package com.biadevcosta.identity.infrastructure.web;

import com.biadevcosta.identity.application.command.RegisterUserCommand;
import com.biadevcosta.identity.application.port.UserRepository;
import com.biadevcosta.identity.application.usecase.RegisterUserUseCase;
import com.biadevcosta.identity.domain.Role;
import com.biadevcosta.identity.domain.User;
import com.biadevcosta.identity.domain.exception.UserNotFoundException;
import com.biadevcosta.identity.infrastructure.web.UserController.RegisterUserRequest;
import com.biadevcosta.identity.infrastructure.web.UserController.UserProfileResponse;
import com.biadevcosta.identity.infrastructure.web.UserController.UserResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    RegisterUserUseCase registerUserUseCase;
    @Mock
    UserRepository userRepository;

    private UserController controller() {
        return new UserController(registerUserUseCase, userRepository);
    }

    private static User doctor() {
        return User.rehydrate("user-1", "doc@x.com", "{argon2id}$h", Role.DOCTOR, "Dr X",
                "119", "CRM-1", "Cardio", LocalDateTime.of(2026, 1, 1, 9, 0));
    }

    @Test
    void register_buildsCommandFromRequest_andMapsResponse() {
        when(registerUserUseCase.execute(any())).thenReturn(doctor());

        UserResponse response = controller().register(new RegisterUserRequest(
                "doc@x.com", "pw", Role.DOCTOR, "Dr X", "119", "CRM-1", "Cardio"));

        ArgumentCaptor<RegisterUserCommand> captor = ArgumentCaptor.forClass(RegisterUserCommand.class);
        verify(registerUserUseCase).execute(captor.capture());
        RegisterUserCommand command = captor.getValue();
        assertThat(command.email()).isEqualTo("doc@x.com");
        assertThat(command.rawPassword()).isEqualTo("pw");
        assertThat(command.role()).isEqualTo(Role.DOCTOR);
        assertThat(command.crm()).isEqualTo("CRM-1");

        assertThat(response).isEqualTo(new UserResponse("user-1", "doc@x.com", "DOCTOR", "Dr X"));
    }

    @Test
    void getById_mapsProfile_withFullNameExposedAsName() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(doctor()));

        UserProfileResponse response = controller().getById("user-1");

        assertThat(response).isEqualTo(new UserProfileResponse("user-1", "Dr X", "doc@x.com", "DOCTOR"));
    }

    @Test
    void getById_unknownId_throwsUserNotFound() {
        when(userRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller().getById("ghost"))
                .isInstanceOf(UserNotFoundException.class);
    }
}
