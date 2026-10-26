package com.gestionproyectos.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Convierte las excepciones de la capa REST en respuestas JSON {"mensaje": "..."},
 * el mismo formato que ya usa AuthController.
 */
@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    private static final Logger logger =
            LoggerFactory.getLogger(ManejadorGlobalExcepciones.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> noEncontrado(RecursoNoEncontradoException e) {
        return respuesta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<Map<String, String>> solicitudInvalida(SolicitudInvalidaException e) {
        return respuesta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validacion(MethodArgumentNotValidException e) {
        String mensaje = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .orElse("Datos no válidos");
        return respuesta(HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> cuerpoIlegible(HttpMessageNotReadableException e) {
        return respuesta(HttpStatus.BAD_REQUEST,
                "El cuerpo de la solicitud no es válido (revisa el JSON y el formato de las fechas AAAA-MM-DD).");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> integridad(DataIntegrityViolationException e) {
        logger.warn("Violación de integridad: {}", e.getMostSpecificCause().getMessage());
        return respuesta(HttpStatus.CONFLICT,
                "La operación viola una restricción de la base de datos (registro duplicado o con datos relacionados).");
    }

    private ResponseEntity<Map<String, String>> respuesta(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of("mensaje", mensaje));
    }
}
