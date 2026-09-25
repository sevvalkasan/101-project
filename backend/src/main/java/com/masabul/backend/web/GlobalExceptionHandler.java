package com.masabul.backend.web;

import com.masabul.backend.security.FirebaseTokenVerificationException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<String> validationErrors = exception.getBindingResult().getFieldErrors().stream()
            .map(this::formatFieldError)
            .toList();
        return error(HttpStatus.BAD_REQUEST, "Validation failed", validationErrors, request.getRequestURI());
    }

    @ExceptionHandler(FirebaseTokenVerificationException.class)
    ResponseEntity<ApiErrorResponse> handleFirebaseToken(FirebaseTokenVerificationException exception, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "Invalid Firebase ID token", List.of(), request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), List.of(), request.getRequestURI());
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ApiErrorResponse> handleIllegalState(IllegalStateException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), List.of(), request.getRequestURI());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception, HttpServletRequest request) {
        List<String> validationErrors = exception.getConstraintViolations().stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .toList();
        return error(HttpStatus.BAD_REQUEST, "Validation failed", validationErrors, request.getRequestURI());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleNotFound(EntityNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), List.of(), request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "Forbidden", List.of(), request.getRequestURI());
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message, List<String> validationErrors, String path) {
        return ResponseEntity.status(status)
            .body(new ApiErrorResponse(OffsetDateTime.now(), status.value(), message, validationErrors, path));
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }
}
