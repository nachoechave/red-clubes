package com.redclubes.backend.config;

import com.redclubes.backend.clubes.ClubNoEncontradoException;
import com.redclubes.backend.socios.SocioNoEncontradoException;
import com.redclubes.backend.usuarios.AccesoDenegadoException;
import com.redclubes.backend.usuarios.AutenticacionRequeridaException;
import com.redclubes.backend.usuarios.CredencialesInvalidasException;
import com.redclubes.backend.usuarios.UsuarioNoEncontradoException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiErrorResponse> manejarHeaderFaltante(
            MissingRequestHeaderException exception,
            HttpServletRequest request
    ) {
        if ("Authorization".equalsIgnoreCase(exception.getHeaderName())) {
            return response(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED",
                    "Se requiere una sesion valida", request);
        }
        return response(HttpStatus.BAD_REQUEST, "MISSING_HEADER",
                "Falta un encabezado obligatorio", request);
    }

    @ExceptionHandler(AutenticacionRequeridaException.class)
    public ResponseEntity<ApiErrorResponse> manejarAutenticacionRequerida(
            AutenticacionRequeridaException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", exception.getMessage(), request);
    }

    @ExceptionHandler({
            SocioNoEncontradoException.class,
            UsuarioNoEncontradoException.class,
            ClubNoEncontradoException.class
    })
    public ResponseEntity<ApiErrorResponse> manejarRecursoNoEncontrado(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ApiErrorResponse> manejarCredencialesInvalidas(
            CredencialesInvalidasException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", exception.getMessage(), request);
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<ApiErrorResponse> manejarAccesoDenegado(
            AccesoDenegadoException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.FORBIDDEN, "ACCESS_DENIED", exception.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> manejarArgumentoInvalido(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.BAD_REQUEST, "BUSINESS_RULE_VIOLATION", exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> manejarErroresDeValidacion(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> errores = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errores.putIfAbsent(error.getField(), error.getDefaultMessage())
        );
        ApiErrorResponse body = ApiErrorResponse.validation(
                HttpStatus.BAD_REQUEST.value(),
                "Los datos enviados no son validos",
                errores,
                request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> manejarConflictoDeIntegridad(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION",
                "La operacion entra en conflicto con datos existentes", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> manejarErrorInesperado(
            Exception exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Ocurrio un error interno", request);
    }

    private ResponseEntity<ApiErrorResponse> response(
            HttpStatus status,
            String error,
            String message,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(status).body(ApiErrorResponse.of(
                status.value(), error, message, request.getRequestURI()
        ));
    }
}
