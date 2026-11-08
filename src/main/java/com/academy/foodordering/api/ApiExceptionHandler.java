package com.academy.foodordering.api;

import com.academy.foodordering.domain.BusinessRejection;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Separates input errors (400) from expected business rejection (422). */
@RestControllerAdvice
public class ApiExceptionHandler {
    public record ApiError(String code, String message, Instant timestamp) {}

    @ExceptionHandler(BusinessRejection.class)
    ResponseEntity<ApiError> business(BusinessRejection exception) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ApiError(exception.code(), exception.getMessage(), Instant.now()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ApiError> invalid(Exception exception) {
        return ResponseEntity.badRequest()
                .body(new ApiError("INVALID_REQUEST",
                        "Request body is malformed or violates the documented input contract", Instant.now()));
    }
}
