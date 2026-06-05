package org.offitec.osp.infrastructure.exception;

import org.offitec.osp.domain.exception.AdminAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AdminAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleAdminAlreadyExistsException(RuntimeException ex){

        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleAMethodArgumentNotValidException(MethodArgumentNotValidException ex){

        return new ResponseEntity<>(Map.of("message", ex.getFieldError().getDefaultMessage()), HttpStatus.BAD_REQUEST);
    }
}
