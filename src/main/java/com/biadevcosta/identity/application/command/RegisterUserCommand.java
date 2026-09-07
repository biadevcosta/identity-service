package com.biadevcosta.identity.application.command;

import com.biadevcosta.identity.domain.Role;

/**
 * Input to {@code RegisterUserUseCase}. {@code rawPassword} is hashed (via the {@code PasswordHasher}
 * port) before the {@code User} is built; {@code crm}/{@code specialty} are only meaningful for a doctor.
 */
public record RegisterUserCommand(
        String email,
        String rawPassword,
        Role role,
        String fullName,
        String phone,
        String crm,
        String specialty) {
}
