package com.biadevcosta.identity.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI document metadata and the {@code bearer-jwt} security scheme so the Swagger UI
 * "Authorize" button lets you paste an access token and call {@code POST /users}.
 * Served at {@code /v3/api-docs} (JSON) and {@code /swagger-ui.html} (UI); both are permitted
 * without authentication in {@code SecurityConfig}.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI identityOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("identity-service API")
                        .version("v1")
                        .description("""
                                Authorization server of the hospital appointment system: password login,
                                RS256 JWT access-token issuance, single-use refresh-token rotation, and
                                ADMIN-only user registration. Errors are RFC 7807 ProblemDetail responses.

                                Quick start: call POST /auth/login with the seeded admin
                                (admin@hospital.local / admin12345), copy accessToken, click Authorize,
                                paste it, then try POST /users.""")
                        .license(new License().name("Proprietary")))
                .components(new Components().addSecuritySchemes("bearer-jwt", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Paste the accessToken returned by POST /auth/login.")));
    }
}
