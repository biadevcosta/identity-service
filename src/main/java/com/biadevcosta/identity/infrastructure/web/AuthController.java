package com.biadevcosta.identity.infrastructure.web;

import com.biadevcosta.identity.application.AuthTokens;
import com.biadevcosta.identity.application.usecase.AuthenticateUseCase;
import com.biadevcosta.identity.application.usecase.RefreshAccessTokenUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public authentication endpoints: password login and refresh-token rotation. */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticateUseCase authenticateUseCase;
    private final RefreshAccessTokenUseCase refreshAccessTokenUseCase;

    public AuthController(AuthenticateUseCase authenticateUseCase,
                          RefreshAccessTokenUseCase refreshAccessTokenUseCase) {
        this.authenticateUseCase = authenticateUseCase;
        this.refreshAccessTokenUseCase = refreshAccessTokenUseCase;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return TokenResponse.from(authenticateUseCase.login(request.email(), request.password()));
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return TokenResponse.from(refreshAccessTokenUseCase.refresh(request.refreshToken()));
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {
        static TokenResponse from(AuthTokens tokens) {
            return new TokenResponse(tokens.accessToken(), tokens.refreshToken(), "Bearer",
                    tokens.expiresInSeconds());
        }
    }
}
