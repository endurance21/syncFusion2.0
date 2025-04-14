package com.syncfusion.exceptions;

import com.syncfusion.dto.response.TJSResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.validation.FieldError;

import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<TJSResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors()
                .stream().map(FieldError::getDefaultMessage).collect(Collectors.toList());
        TJSResponse<Object, Object> responseBody = TJSResponse.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message("Validation Failed")
                .errors(errors)
                .build();
        return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TJSException.class)
    public ResponseEntity<TJSResponse> handleTJSException(TJSException ex) {
        TJSResponse<Object, Object> responseBody = TJSResponse.builder()
                .status(ex.getStatusCode())
                .message(ex.getMessage())
                .errors(Collections.singletonList(ex.getMessage()))
                .build();
        return new ResponseEntity<>(responseBody, new HttpHeaders(), ex.getStatusCode());
    }
}
