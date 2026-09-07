package com.biadevcosta.identity.infrastructure.web;

import com.biadevcosta.identity.application.AuthTokens;
import com.biadevcosta.identity.application.usecase.AuthenticateUseCase;
import com.biadevcosta.identity.application.usecase.RefreshAccessTokenUseCase;
import com.biadevcosta.identity.infrastructure.web.AuthController.LoginRequest;
import com.biadevcosta.identity.infrastructure.web.AuthController.RefreshRequest;
import com.biadevcosta.identity.infrastructure.web.AuthController.TokenResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    AuthenticateUseCase authenticateUseCase;
    @Mock
    RefreshAccessTokenUseCase refreshAccessTokenUseCase;

    private AuthController controller() {
        return new AuthController(authenticateUseCase, refreshAccessTokenUseCase);
    }

    @Test
    void login_delegatesAndMapsToBearerResponse() {
        when(authenticateUseCase.login("me@x.com", "pw"))
                .thenReturn(new AuthTokens("access", "refresh", 3600));

        TokenResponse response = controller().login(new LoginRequest("me@x.com", "pw"));

        assertThat(response).isEqualTo(new TokenResponse("access", "refresh", "Bearer", 3600));
    }

    @Test
    void refresh_delegatesAndMapsToBearerResponse() {
        when(refreshAccessTokenUseCase.refresh("raw"))
                .thenReturn(new AuthTokens("access2", "refresh2", 3600));

        TokenResponse response = controller().refresh(new RefreshRequest("raw"));

        assertThat(response).isEqualTo(new TokenResponse("access2", "refresh2", "Bearer", 3600));
    }
}
