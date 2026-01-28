package com.infragest.infra_notifications_service.exception;

/**
 * Excepción personalizada para manejar errores relacionados con el servicio de SendGrid.
 */
public class SengridException extends RuntimeException {

    /**
     * Enum que representa los diferentes tipos de errores de usuario.
     */
    public enum Type {
        API_ERROR,                 // Error general no especificado desde la API de SendGrid
        INVALID_REQUEST,           // La solicitud enviada a SendGrid era inválida
        AUTHENTICATION_FAILURE,    // Fallo de autenticación con la API de SendGrid
        RESOURCE_NOT_FOUND,        // Recurso o endpoint no encontrado
        QUOTA_EXCEEDED,            // Se superó el límite de envíos permitidos
        SERVICE_UNAVAILABLE        // El servicio de SendGrid está temporalmente no disponible
    }

    private final Type type;

    /**
     * Crea una nueva SengridException con el mensaje y tipo especificados.
     *
     * @param message El mensaje descriptivo de la excepción.
     * @param type El tipo de error relacionado con SendGrid.
     */
    public SengridException(String message, Type type) {
        super(message);
        this.type = type;
    }

    /**
     * Obtiene el tipo de error relacionado con SendGrid.
     *
     * @return El tipo de error {@link Type}.
     */
    public Type getType() {
        return type;
    }
}
