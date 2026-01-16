package com.findoraai.giftfinder.config.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(
            DuplicateEmailException ex, HttpServletRequest request) {
        String requestId = getOrGenerateRequestId(request);
        log.warn("Duplicate email attempt - requestId: {}, email: {}", requestId, ex.getEmail());
        
        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .error(ex.getMessage())
                .code("DUPLICATE_EMAIL")
                .requestId(requestId)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String requestId = getOrGenerateRequestId(request);
        
        Map<String, String> validationErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            validationErrors.put(error.getField(), error.getDefaultMessage());
        }
        
        log.warn("Validation failed - requestId: {}, errors: {}", requestId, validationErrors);
        
        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .error("Validation failed")
                .code("VALIDATION_ERROR")
                .details(validationErrors)
                .requestId(requestId)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        String requestId = getOrGenerateRequestId(request);
        log.warn("Authentication failed - requestId: {}, message: {}", requestId, ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .error("Invalid credentials")
                .code("AUTHENTICATION_FAILED")
                .requestId(requestId)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        String requestId = getOrGenerateRequestId(request);
        log.warn("Bad credentials - requestId: {}", requestId);
        
        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .error("Invalid credentials")
                .code("BAD_CREDENTIALS")
                .requestId(requestId)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        String requestId = getOrGenerateRequestId(request);
        log.warn("Access denied - requestId: {}, message: {}", requestId, ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .error("Access denied")
                .code("ACCESS_DENIED")
                .requestId(requestId)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        String requestId = getOrGenerateRequestId(request);
        log.error("Data integrity violation - requestId: {}, message: {}", requestId, ex.getMessage());
        
        // Check if it's a unique constraint violation
        String message = ex.getMessage();
        if (message != null && message.toLowerCase().contains("duplicate") 
                && message.toLowerCase().contains("email")) {
            return handleDuplicateEmail(
                new DuplicateEmailException("Email already registered"), 
                request);
        }
        
        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .error("Data integrity violation")
                .code("DATA_INTEGRITY_VIOLATION")
                .requestId(requestId)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ErrorResponse> handleSQLException(
            SQLException ex, HttpServletRequest request) {
        String requestId = getOrGenerateRequestId(request);
        log.error("Database error - requestId: {}, SQLState: {}, message: {}", 
                requestId, ex.getSQLState(), ex.getMessage(), ex);
        
        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .error("Database service unavailable")
                .code("DATABASE_ERROR")
                .requestId(requestId)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        String requestId = getOrGenerateRequestId(request);
        log.error("Unexpected error - requestId: {}, type: {}, message: {}", 
                requestId, ex.getClass().getSimpleName(), ex.getMessage(), ex);
        
        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .error("An unexpected error occurred. Please try again later.")
                .code("INTERNAL_ERROR")
                .requestId(requestId)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private String getOrGenerateRequestId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-ID");
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }
        return requestId;
    }
}
