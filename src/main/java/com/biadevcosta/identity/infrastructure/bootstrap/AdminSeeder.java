package com.biadevcosta.identity.infrastructure.bootstrap;

import com.biadevcosta.identity.application.command.RegisterUserCommand;
import com.biadevcosta.identity.application.port.UserRepository;
import com.biadevcosta.identity.application.usecase.RegisterUserUseCase;
import com.biadevcosta.identity.domain.Role;
import com.biadevcosta.identity.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Creates the bootstrap ADMIN user on startup when it does not exist yet, so the system has an
 * account that can register everyone else. Idempotent: skips when the admin email is already taken.
 * The password is hashed by the live {@code PasswordHasher}, so its parameters always match.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final RegisterUserUseCase registerUserUseCase;
    private final UserRepository userRepository;
    private final String adminEmail;
    private final String adminPassword;

    public AdminSeeder(RegisterUserUseCase registerUserUseCase,
                       UserRepository userRepository,
                       @Value("${app.admin.email}") String adminEmail,
                       @Value("${app.admin.password}") String adminPassword) {
        this.registerUserUseCase = registerUserUseCase;
        this.userRepository = userRepository;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(User.normalizeEmail(adminEmail))) {
            return;
        }
        registerUserUseCase.execute(new RegisterUserCommand(
                adminEmail, adminPassword, Role.ADMIN, "System Admin", null, null, null));
        log.info("Seeded bootstrap admin user {}", User.normalizeEmail(adminEmail));
    }
}
