package com.biadevcosta.identity.infrastructure.config;

import com.biadevcosta.identity.application.port.PasswordHasher;
import com.biadevcosta.identity.application.port.RefreshTokenHasher;
import com.biadevcosta.identity.application.port.RefreshTokenRepository;
import com.biadevcosta.identity.application.port.TokenIssuer;
import com.biadevcosta.identity.application.port.UserRepository;
import com.biadevcosta.identity.application.usecase.AuthenticateUseCase;
import com.biadevcosta.identity.application.usecase.RefreshAccessTokenUseCase;
import com.biadevcosta.identity.application.usecase.RegisterUserUseCase;
import com.biadevcosta.identity.application.usecase.TokenMinter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Wires the plain-POJO use cases as beans (they carry no Spring annotations themselves). */
@Configuration
public class UseCaseConfig {

    @Bean
    TokenMinter tokenMinter(TokenIssuer tokenIssuer,
                            RefreshTokenRepository refreshTokenRepository,
                            RefreshTokenHasher refreshTokenHasher,
                            JwtProperties jwt,
                            Clock clock) {
        return new TokenMinter(tokenIssuer, refreshTokenRepository, refreshTokenHasher,
                jwt.refreshTtlSeconds(), clock);
    }

    @Bean
    RegisterUserUseCase registerUserUseCase(UserRepository userRepository, PasswordHasher passwordHasher) {
        return new RegisterUserUseCase(userRepository, passwordHasher);
    }

    @Bean
    AuthenticateUseCase authenticateUseCase(UserRepository userRepository,
                                            PasswordHasher passwordHasher,
                                            TokenMinter tokenMinter) {
        return new AuthenticateUseCase(userRepository, passwordHasher, tokenMinter);
    }

    @Bean
    RefreshAccessTokenUseCase refreshAccessTokenUseCase(RefreshTokenRepository refreshTokenRepository,
                                                       RefreshTokenHasher refreshTokenHasher,
                                                       UserRepository userRepository,
                                                       TokenMinter tokenMinter,
                                                       Clock clock) {
        return new RefreshAccessTokenUseCase(refreshTokenRepository, refreshTokenHasher,
                userRepository, tokenMinter, clock);
    }
}
