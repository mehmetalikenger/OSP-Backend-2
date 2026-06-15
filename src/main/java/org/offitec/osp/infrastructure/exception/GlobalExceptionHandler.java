package org.offitec.osp.infrastructure.exception;

import org.offitec.osp.domain.exception.*;
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

    @ExceptionHandler(ModelAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleModelAlreadyExistsException(RuntimeException ex){

        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(CompressorDoesntExistException.class)
    public ResponseEntity<Map<String, String>> handleCompressorDoesntExistException(RuntimeException ex){

        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(CompressorSpecsDoesntExistException.class)
    public ResponseEntity<Map<String, String>> handleCompressorSpecsDoesntExistException(RuntimeException ex){

        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(EvaporatorDoesntExistException.class)
    public ResponseEntity<Map<String, String>> handleEvaporatorDoesntExistException(RuntimeException ex){

        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(EvaporatorSpecsDoesntExistException.class)
    public ResponseEntity<Map<String, String>> handleEvaporatorSpecsDoesntExistException(RuntimeException ex){
        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(CondenserDoesntExistException.class)
    public ResponseEntity<Map<String, String>> handleCondenserDoesntExistException(RuntimeException ex){
        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(CondenserSpecsDoesntExistException.class)
    public ResponseEntity<Map<String, String>> handleCondenserSpecsDoesntExistException(RuntimeException ex){
        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ExpansionValveDoesntExistException.class)
    public ResponseEntity<Map<String, String>> handleExpansionValveDoesntExistException(RuntimeException ex){
        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ExpansionValveSpecsDoesntExistException.class)
    public ResponseEntity<Map<String, String>> handleExpansionValveSpecsDoesntExistException(RuntimeException ex){
        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(FourWayReversingValveDoesntExistException.class)
    public ResponseEntity<Map<String, String>> handleFourWayReversingValveDoesntExistException(RuntimeException ex){
        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(FourWayReversingValveSpecsDoesntExistException.class)
    public ResponseEntity<Map<String, String>> handleFourWayReversingValveSpecsDoesntExistException(RuntimeException ex){
        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ChassisDoesntExistException.class)
    public ResponseEntity<Map<String, String>> handleChassisDoesntExistException(RuntimeException ex){
        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(RefrigerantDoesntExistException.class)
    public ResponseEntity<Map<String, String>> handleRefrigerantDoesntExistException(RuntimeException ex){
        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.CONFLICT);
    }
}
