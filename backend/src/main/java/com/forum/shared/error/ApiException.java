package com.forum.shared.error;

import org.springframework.http.HttpStatus;

/**
 * Excepción de dominio que transporta el estado HTTP con el que debe responderse.
 * Se traduce a un {@link org.springframework.http.ProblemDetail} en
 * {@link GlobalExceptionHandler}. Usar las factorías estáticas para los casos comunes.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    /** 401: credenciales ausentes o inválidas. */
    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, message);
    }

    /** 404: el recurso no existe o no es visible para quien consulta. */
    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    /** 403: autenticado pero sin permiso (incluye usuario suspendido intentando escribir). */
    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, message);
    }

    /** 409: conflicto (duplicado, transición de estado inválida). */
    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }

    /** 400: pedido inválido más allá de la validación de Bean Validation. */
    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    /** 413: archivo demasiado grande. */
    public static ApiException payloadTooLarge(String message) {
        return new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, message);
    }

    /** 415: tipo de archivo no permitido o no decodificable. */
    public static ApiException unsupportedMediaType(String message) {
        return new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, message);
    }
}
