package com.biadevcosta.identity.application.usecase;

import com.biadevcosta.identity.application.command.RegisterUserCommand;
import com.biadevcosta.identity.application.port.PasswordHasher;
import com.biadevcosta.identity.application.port.UserRepository;
import com.biadevcosta.identity.domain.User;
import com.biadevcosta.identity.domain.exception.EmailAlreadyUsedException;
import com.biadevcosta.identity.domain.exception.InvalidUserException;

/**
 * Registers a new user. Only an ADMIN reaches this use case (the role gate lives on the controller).
 * Order: guard the raw password, hash it, build the {@link User} (domain invariants), check the
 * email is free, then persist. The password is hashed before the uniqueness check on purpose, so the
 * work done does not leak whether the email already exists.
 */
public class RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public RegisterUserUseCase(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    public User execute(RegisterUserCommand command) {
        if (command.rawPassword() == null || command.rawPassword().isBlank()) {
            throw new InvalidUserException("password is required");
        }
        String passwordHash = passwordHasher.hash(command.rawPassword());
        User user = User.register(command.email(), passwordHash, command.role(), command.fullName(),
                command.phone(), command.crm(), command.specialty());
        if (userRepository.existsByEmail(user.email())) {
            throw new EmailAlreadyUsedException(user.email());
        }
        return userRepository.save(user);
    }
}
