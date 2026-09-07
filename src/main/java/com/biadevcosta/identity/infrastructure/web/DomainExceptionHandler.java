package com.biadevcosta.identity.infrastructure.web;

import com.biadevcosta.identity.domain.exception.EmailAlreadyUsedException;
import com.biadevcosta.identity.domain.exception.IdentityException;
import com.biadevcosta.identity.domain.exception.InvalidCredentialsException;
import com.biadevcosta.identity.domain.exception.InvalidRefreshTokenException;
import com.biadevcosta.identity.domain.exception.InvalidUserException;
import com.biadevcosta.identity.domain.exception.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/** Maps domain exceptions and bean-validation failures to RFC 7807 {@link ProblemDetail} responses. */
@RestControllerAdvice
public class DomainExceptionHandler {

    @ExceptionHandler({InvalidCredentialsException.class, InvalidRefreshTokenException.class})
    ProblemDetail onUnauthorized(IdentityException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(EmailAlreadyUsedException.class)
    ProblemDetail onConflict(EmailAlreadyUsedException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    ProblemDetail onNotFound(UserNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({InvalidUserException.class, IdentityException.class})
    ProblemDetail onBadRequest(IdentityException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail onValidation(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                detail.isBlank() ? "validation failed" : detail);
    }
}
