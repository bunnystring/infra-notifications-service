package com.infragest.infra_notifications_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja errores de validación generados por {@code @Valid}.
     *
     * @param ex excepción de validación generada por el framework Spring
     * @return respuesta con detalles del error
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.error("Validation Exception occurred: {}", message);
        return buildErrorResponse("Validation Error", HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Maneja excepciones de SendGrid utilizando la clase personalizada SengridException.
     *
     * @param ex excepción de tipo SengridException lanzada por el servicio de notificaciones.
     * @return respuesta con detalles sobre el error, incluyendo el tipo de error.
     */
    @ExceptionHandler(SengridException.class)
    public ResponseEntity<Map<String, Object>> handleSengridException(SengridException ex) {
        // Registrar el tipo y mensaje del error en los logs
        log.error("SendGrid Exception occurred: Type: {}, Message: {}", ex.getType(), ex.getMessage());

        // Construir una respuesta específica para SendGrid
        return buildErrorResponse(
                "SendGrid Error - " + ex.getType().name(), // Incluimos el tipo de error
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
    }

    /**
     * Maneja excepciones no controladas y devuelve 500 (Internal Server Error).
     *
     * @param ex excepción no controlada que ocurrió durante el procesamiento
     * @return respuesta genérica con un código HTTP 500 y un identificador único de error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericsException(Exception ex) {
        String errorId = UUID.randomUUID().toString();
        log.error("Unhandled exception occurred. Error ID: {}. Message: {}", errorId, ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "timestamp", LocalDateTime.now(),
                        "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "error", "Internal Server Error",
                        "message", "An unexpected error occurred. Error ID: " + errorId
                ));
    }

    /**
     * Método auxiliar para construir respuestas de error consistentes.
     *
     * @param error el nombre o tipo del error
     * @param status el código de estado HTTP asociado al error
     * @param message un mensaje descriptivo sobre el error
     * @return ResponseEntity con los detalles del error
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(String error, HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(Map.of(
                        "timestamp", LocalDateTime.now(),
                        "status", status.value(),
                        "error", error,
                        "message", message
                ));
    }

}
