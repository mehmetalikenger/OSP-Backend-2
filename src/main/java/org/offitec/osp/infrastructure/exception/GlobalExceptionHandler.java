package org.offitec.osp.infrastructure.exception;

import org.offitec.osp.domain.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    // --- 401 Unauthorized: authentication failures ---

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFoundException(RuntimeException ex){

        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(PasswordsDontMatchException.class)
    public ResponseEntity<Map<String, String>> handlePasswordsDontMatchException(RuntimeException ex){

        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    // --- 400 Bad Request: invalid input ---

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleAMethodArgumentNotValidException(MethodArgumentNotValidException ex){

        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : ex.getMessage();

        return new ResponseEntity<>(Map.of("message", message), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex){

        return new ResponseEntity<>(Map.of("message", "Invalid value provided."), HttpStatus.BAD_REQUEST);
    }

    // --- 409 Conflict: resource already exists ---

    @ExceptionHandler(AdminAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleAdminAlreadyExistsException(RuntimeException ex){

        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ModelAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleModelAlreadyExistsException(RuntimeException ex){

        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.CONFLICT);
    }

    // --- 404 Not Found: referenced resource does not exist ---

    @ExceptionHandler(CompressorDoesntExistException.class)
    public ResponseEntity<Map<String, String>> handleCompressorDoesntExistException(RuntimeException ex){

        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CompressorSpecsDoesntExistException.class)
    public ResponseEntity<Map<String, String>> handleCompressorSpecsDoesntExistException(RuntimeException ex){

        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(EvaporatorDoesntExistException.class)
    public ResponseEntity<Map<String, String>> handleEvaporatorDoesntExistException(RuntimeException ex){

        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(EvaporatorSpecsDoesntExistException.class)
    public ResponseEntity<Map<String, String>> handleEvaporatorSpecsDoesntExistException(RuntimeException ex){
        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CondenserDoesntExistException.class)
    public ResponseEntity<Map<String, String>> handleCondenserDoesntExistException(RuntimeException ex){
        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CondenserSpecsDoesntExistException.class)
    public ResponseEntity<Map<String, String>> handleCondenserSpecsDoesntExistException(RuntimeException ex){
        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ExpansionValveDoesntExistException.class)
    public ResponseEntity<Map<String, String>> handleExpansionValveDoesntExistException(RuntimeException ex){
        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ExpansionValveSpecsDoesntExistException.class)
    public ResponseEntity<Map<String, String>> handleExpansionValveSpecsDoesntExistException(RuntimeException ex){
        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(FourWayReversingValveDoesntExistException.class)
    public ResponseEntity<Map<String, String>> handleFourWayReversingValveDoesntExistException(RuntimeException ex){
        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(FourWayReversingValveSpecsDoesntExistException.class)
    public ResponseEntity<Map<String, String>> handleFourWayReversingValveSpecsDoesntExistException(RuntimeException ex){
        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ChassisDoesntExistException.class)
    public ResponseEntity<Map<String, String>> handleChassisDoesntExistException(RuntimeException ex){
        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(RefrigerantDoesntExistException.class)
    public ResponseEntity<Map<String, String>> handleRefrigerantDoesntExistException(RuntimeException ex){
        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UnitDoesntExistException.class)
    public ResponseEntity<Map<String, String>> handleUnitDoesntExistException(RuntimeException ex){
        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.NOT_FOUND);
    }
}
