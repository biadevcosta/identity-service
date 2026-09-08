package com.biadevcosta.identity.infrastructure.web;

import com.biadevcosta.identity.application.command.RegisterUserCommand;
import com.biadevcosta.identity.application.port.UserRepository;
import com.biadevcosta.identity.application.usecase.RegisterUserUseCase;
import com.biadevcosta.identity.domain.Role;
import com.biadevcosta.identity.domain.User;
import com.biadevcosta.identity.domain.exception.UserNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * User management. {@code POST /users} registers a user and is restricted to ADMIN; {@code GET /users/{id}}
 * is a public profile lookup used by the other services to resolve names.
 */
@RestController
@RequestMapping("/users")
@Tag(name = "users", description = "Registration (ADMIN only) and public profile lookup.")
public class UserController {

    private final RegisterUserUseCase registerUserUseCase;
    private final UserRepository userRepository;

    public UserController(RegisterUserUseCase registerUserUseCase, UserRepository userRepository) {
        this.registerUserUseCase = registerUserUseCase;
        this.userRepository = userRepository;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Register a user (ADMIN only)",
            description = "Roles: DOCTOR (requires crm + specialty), NURSE, PATIENT, ADMIN. "
                    + "401 without a token, 403 with a non-ADMIN token, 409 on a duplicate email.")
    public UserResponse register(@Valid @RequestBody RegisterUserRequest request) {
        User user = registerUserUseCase.execute(new RegisterUserCommand(
                request.email(), request.password(), request.role(), request.fullName(),
                request.phone(), request.crm(), request.specialty()));
        return UserResponse.from(user);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Look up a user profile by id",
            description = "Public route used by scheduling-service / history-service to resolve names. 404 if unknown.")
    public UserProfileResponse getById(@PathVariable String id) {
        return userRepository.findById(id)
                .map(UserProfileResponse::from)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public record RegisterUserRequest(
            @NotBlank String email,
            @NotBlank String password,
            @NotNull Role role,
            @NotBlank String fullName,
            String phone,
            String crm,
            String specialty) {
    }

    public record UserResponse(String id, String email, String role, String fullName) {
        static UserResponse from(User user) {
            return new UserResponse(user.id(), user.email(), user.role().name(), user.fullName());
        }
    }

    public record UserProfileResponse(String id, String name, String email, String role) {
        static UserProfileResponse from(User user) {
            return new UserProfileResponse(user.id(), user.fullName(), user.email(), user.role().name());
        }
    }
}
