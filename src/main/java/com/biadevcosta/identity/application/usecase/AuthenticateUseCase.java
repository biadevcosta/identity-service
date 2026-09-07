package com.biadevcosta.identity.application.usecase;

import com.biadevcosta.identity.application.AuthTokens;
import com.biadevcosta.identity.application.port.PasswordHasher;
import com.biadevcosta.identity.application.port.UserRepository;
import com.biadevcosta.identity.domain.User;
import com.biadevcosta.identity.domain.exception.InvalidCredentialsException;

/**
 * Logs a user in with email + password. An unknown email and a wrong password fail identically
 * ({@link InvalidCredentialsException}) so the caller cannot tell which one was wrong.
 */
public class AuthenticateUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenMinter tokenMinter;

    public AuthenticateUseCase(UserRepository userRepository,
                               PasswordHasher passwordHasher,
                               TokenMinter tokenMinter) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenMinter = tokenMinter;
    }

    public AuthTokens login(String email, String rawPassword) {
        User user = userRepository.findByEmail(User.normalizeEmail(email))
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordHasher.matches(rawPassword, user.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        return tokenMinter.mintFor(user);
    }
}
