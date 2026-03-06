package com.innowise.orderservice.domain.exception;

import com.innowise.orderservice.web.dto.ErrorResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<String> handleAccessDeniedException(Exception ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Access denied: You do not have permission to access this resource");
    }

    @ExceptionHandler(ItemNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleItemNotFound(
            ItemNotFoundException ex,
            WebRequest request
    ) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleOrderNotFound(
            OrderNotFoundException ex,
            WebRequest request
    ) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND, request);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(
            MethodArgumentNotValidException ex,
            WebRequest request
    ) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return buildErrorResponse(message, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleDataIntegrity(
            DataIntegrityViolationException ex,
            WebRequest request
    ) {
        log.warn("DB constraint violation", ex);
        return buildErrorResponse(
                "Database constraint violation",
                HttpStatus.CONFLICT,
                request
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleMalformedJson(
            HttpMessageNotReadableException ex,
            WebRequest request
    ) {
        log.warn("Malformed JSON: {}", ex.getMessage());
        return buildErrorResponse(
                "Invalid request body: JSON is malformed.",
                HttpStatus.BAD_REQUEST,
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleOther(
            Exception ex,
            WebRequest request
    ) {
        log.error("Unhandled exception", ex);
        return buildErrorResponse(
                "Internal server error",
                HttpStatus.INTERNAL_SERVER_ERROR,
                request
        );
    }

    private ResponseEntity<ErrorResponseDto> buildErrorResponse(
            String message,
            HttpStatus status,
            WebRequest request
    ) {
        ErrorResponseDto dto = new ErrorResponseDto(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getDescription(false).replace("uri=", "")
        );

        return ResponseEntity.status(status).body(dto);
    }



    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponseDto> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex,
            WebRequest request
    ) {
        log.warn("Unsupported media type: {}", ex.getContentType());

        String message = String.format(
                "Content-Type '%s' is not supported. Please use application/json",
                ex.getContentType()
        );

        return buildErrorResponse(
                message,
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                request
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        String message = String.format("Параметр '%s' должен быть типа '%s'. Передано некорректное значение: '%s'",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "неизвестно",
                ex.getValue());

        return buildErrorResponse(
                message,
                HttpStatus.BAD_REQUEST,
                request
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, WebRequest request) {
        String message = String.format("HTTP метод '%s' не поддерживается для этого адреса. Поддерживаемые методы: %s",
                ex.getMethod(),
                ex.getSupportedHttpMethods());

        return buildErrorResponse(
                message,
                HttpStatus.METHOD_NOT_ALLOWED,
                request
        );
    }

    @ExceptionHandler(ExternalServiceUnavailableException.class)
    public ResponseEntity<ErrorResponseDto> handleExternalServiceUnavailable(
            ExternalServiceUnavailableException ex,
            WebRequest request
    ) {
        log.warn("External service unavailable: {}", ex.getMessage());
        return buildErrorResponse(
                ex.getMessage(),
                HttpStatus.SERVICE_UNAVAILABLE,
                request
        );
    }
}
