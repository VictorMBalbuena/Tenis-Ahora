package com.tenisahora.shared.exception;

import com.tenisahora.infrastructure.mercadopago.PasarelaNoDisponibleException;
import com.tenisahora.shared.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({BadCredentialsException.class, DisabledException.class, UsernameNotFoundException.class})
    public ResponseEntity<ErrorResponse> manejarAutenticacion(RuntimeException ex, HttpServletRequest request) {
        // Mismo mensaje para contraseña incorrecta, cuenta inexistente o deshabilitada: no revela cuál fue
        return responder(HttpStatus.UNAUTHORIZED, ErrorResponse.de(
                CodigoError.CREDENCIALES_INVALIDAS, "Credenciales invalidas", request.getRequestURI()));
    }

    @ExceptionHandler(DemasiadosIntentosException.class)
    public ResponseEntity<ErrorResponse> manejarDemasiadosIntentos(DemasiadosIntentosException ex, HttpServletRequest request) {
        // 429 con Retry-After: el front sabe cuánto esperar y el navegador no reintenta antes
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.segundosDeEspera()))
                .body(deNegocio(ex, request));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> manejarNoEncontrado(ErrorApiException ex, HttpServletRequest request) {
        return responder(HttpStatus.NOT_FOUND, deNegocio(ex, request));
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ErrorResponse> manejarConflicto(ErrorApiException ex, HttpServletRequest request) {
        return responder(HttpStatus.CONFLICT, deNegocio(ex, request));
    }

    @ExceptionHandler(ReglaDeNegocioException.class)
    public ResponseEntity<ErrorResponse> manejarReglaDeNegocio(ErrorApiException ex, HttpServletRequest request) {
        return responder(HttpStatus.UNPROCESSABLE_ENTITY, deNegocio(ex, request));
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ErrorResponse> manejarSolicitudInvalida(ErrorApiException ex, HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST, deNegocio(ex, request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> campos.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return responder(HttpStatus.BAD_REQUEST, ErrorResponse.conCampos(
                CodigoError.VALIDACION_FALLIDA, "Hay datos que corregir", campos, request.getRequestURI()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> manejarParametros(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> {
            String ruta = v.getPropertyPath().toString();
            campos.putIfAbsent(ruta.substring(ruta.lastIndexOf('.') + 1), v.getMessage());
        });
        return responder(HttpStatus.BAD_REQUEST, ErrorResponse.conCampos(
                CodigoError.VALIDACION_FALLIDA, "Hay datos que corregir", campos, request.getRequestURI()));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> manejarValidacionDeParametros(HandlerMethodValidationException ex,
                                                                       HttpServletRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(resultado -> {
            String nombre = resultado.getMethodParameter().getParameterName();
            resultado.getResolvableErrors().stream().findFirst()
                    .ifPresent(error -> campos.putIfAbsent(nombre, error.getDefaultMessage()));
        });
        return responder(HttpStatus.BAD_REQUEST, ErrorResponse.conCampos(
                CodigoError.VALIDACION_FALLIDA, "Hay datos que corregir", campos, request.getRequestURI()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> manejarPedidoMalFormado(Exception ex, HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST, ErrorResponse.de(
                CodigoError.PEDIDO_MAL_FORMADO, "El pedido no tiene el formato esperado", request.getRequestURI()));
    }

    @ExceptionHandler(PasarelaNoDisponibleException.class)
    public ResponseEntity<ErrorResponse> manejarPasarelaCaida(PasarelaNoDisponibleException ex, HttpServletRequest request) {
        return responder(HttpStatus.SERVICE_UNAVAILABLE, ErrorResponse.de(
                CodigoError.PASARELA_NO_DISPONIBLE, "La pasarela de pagos no contestó", request.getRequestURI()));
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> manejarArchivoDemasiadoGrande(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return responder(HttpStatus.UNPROCESSABLE_ENTITY, ErrorResponse.de(
                CodigoError.FOTO_INVALIDA, "La foto no puede pesar más de 2 MB", request.getRequestURI()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> manejarIntegridad(DataIntegrityViolationException ex, HttpServletRequest request) {
        // No se devuelve el detalle de la base: puede mostrar nombres de tablas y columnas
        return responder(HttpStatus.CONFLICT, ErrorResponse.de(
                CodigoError.CONFLICTO_DE_DATOS, "La operación choca con datos que ya existen o están en uso", request.getRequestURI()));
    }

    private ErrorResponse deNegocio(ErrorApiException ex, HttpServletRequest request) {
        if (ex.getCampos() != null) {
            return ErrorResponse.conCampos(ex.getCodigoError(), ex.getMessage(), ex.getCampos(), request.getRequestURI());
        }
        return ErrorResponse.de(ex.getCodigoError(), ex.getMessage(), request.getRequestURI());
    }

    private ResponseEntity<ErrorResponse> responder(HttpStatus estado, ErrorResponse cuerpo) {
        return ResponseEntity.status(estado).body(cuerpo);
    }
}
