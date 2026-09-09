package com.portfolio.prestamos.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<Map<String, Object>> manejarNegocio(NegocioException ex) {
        return ResponseEntity.status(ex.getStatus()).body(cuerpoError(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Solicitud invalida");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(cuerpoError(mensaje));
    }

    @ExceptionHandler(SimulacionCobolException.class)
    public ResponseEntity<Map<String, Object>> manejarErrorCobol(SimulacionCobolException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(cuerpoError(ex.getMessage()));
    }

    private Map<String, Object> cuerpoError(String mensaje) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("timestamp", Instant.now());
        cuerpo.put("error", mensaje);
        return cuerpo;
    }
}
