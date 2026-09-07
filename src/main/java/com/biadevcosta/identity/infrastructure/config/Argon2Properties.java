package com.biadevcosta.identity.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds {@code security.password.argon2.*} from {@code application.yaml} (Argon2id parameters). */
@ConfigurationProperties(prefix = "security.password.argon2")
public record Argon2Properties(
        int saltLength,
        int hashLength,
        int parallelism,
        int memoryKb,
        int iterations) {
}
