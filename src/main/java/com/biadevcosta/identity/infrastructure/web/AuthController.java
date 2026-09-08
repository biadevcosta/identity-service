package com.biadevcosta.identity.infrastructure.web;

import com.biadevcosta.identity.application.AuthTokens;
import com.biadevcosta.identity.application.usecase.AuthenticateUseCase;
import com.biadevcosta.identity.application.usecase.RefreshAccessTokenUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public authentication endpoints: password login and refresh-token rotation. */
@RestController
@RequestMapping("/auth")
@Tag(name = "auth", description = "Password login and single-use refresh-token rotation (public).")
public class AuthController {

    private final AuthenticateUseCase authenticateUseCase;
    private final RefreshAccessTokenUseCase refreshAccessTokenUseCase;

    public AuthController(AuthenticateUseCase authenticateUseCase,
                          RefreshAccessTokenUseCase refreshAccessTokenUseCase) {
        this.authenticateUseCase = authenticateUseCase;
        this.refreshAccessTokenUseCase = refreshAccessTokenUseCase;
    }

    @PostMapping("/login")
    @Operation(summary = "Log in with email + password",
            description = "Returns a fresh access/refresh token pair. Unknown email and wrong password fail identically (401).")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return TokenResponse.from(authenticateUseCase.login(request.email(), request.password()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a refresh token for a new pair",
            description = "The presented refresh token is revoked (rotation); reusing it returns 401.")
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
