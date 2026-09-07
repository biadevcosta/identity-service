package com.biadevcosta.identity.infrastructure.web;

import com.biadevcosta.identity.domain.exception.EmailAlreadyUsedException;
import com.biadevcosta.identity.domain.exception.InvalidCredentialsException;
import com.biadevcosta.identity.domain.exception.InvalidRefreshTokenException;
import com.biadevcosta.identity.domain.exception.InvalidUserException;
import com.biadevcosta.identity.domain.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

class DomainExceptionHandlerTest {

    private final DomainExceptionHandler handler = new DomainExceptionHandler();

    @Test
    void invalidCredentials_is401_withGenericMessage() {
        ProblemDetail problem = handler.onUnauthorized(new InvalidCredentialsException());

        assertThat(problem.getStatus()).isEqualTo(401);
        assertThat(problem.getDetail()).isEqualTo("Invalid credentials");
    }

    @Test
    void invalidRefreshToken_is401() {
        assertThat(handler.onUnauthorized(new InvalidRefreshTokenException()).getStatus()).isEqualTo(401);
    }

    @Test
    void emailAlreadyUsed_is409() {
        assertThat(handler.onConflict(new EmailAlreadyUsedException("a@b.co")).getStatus()).isEqualTo(409);
    }

    @Test
    void userNotFound_is404() {
        assertThat(handler.onNotFound(new UserNotFoundException("x")).getStatus()).isEqualTo(404);
    }

    @Test
    void invalidUser_is400() {
        assertThat(handler.onBadRequest(new InvalidUserException("bad")).getStatus()).isEqualTo(400);
    }

    @Test
    void validationError_is400() {
        MethodArgumentNotValidException exception = Mockito.mock(MethodArgumentNotValidException.class);
        Mockito.when(exception.getBindingResult())
                .thenReturn(new BeanPropertyBindingResult(new Object(), "request"));

        ProblemDetail problem = handler.onValidation(exception);

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getDetail()).isEqualTo("validation failed");
    }
}
