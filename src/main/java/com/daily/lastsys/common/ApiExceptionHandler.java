package com.daily.lastsys.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApiValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleApiValidation(ApiValidationException exception) {
        return new ValidationErrorResponse(exception.errors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleInvalidRequest(MethodArgumentNotValidException exception) {
        List<String> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage() == null ? "입력한 내용을 확인해주세요." : error.getDefaultMessage())
                .distinct()
                .toList();

        if (errors.isEmpty()) {
            errors = List.of("입력한 내용을 확인해주세요.");
        }

        return new ValidationErrorResponse(errors);
    }
}
