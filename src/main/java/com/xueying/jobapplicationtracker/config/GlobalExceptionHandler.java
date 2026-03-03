package com.xueying.jobapplicationtracker.config;

import com.xueying.jobapplicationtracker.utils.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converts uncaught API exceptions into a consistent Result payload.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<String>> handle(Exception ex) {
        return new ResponseEntity<>(Result.fail(ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

