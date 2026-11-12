/**
 * Manejo de errores centralizado: GlobalExceptionHandler (@RestControllerAdvice),
 * RecursoNoEncontradoException (404), ConflictoException (409), ReglaDeNegocioException (422, con
 * un código como RESERVA_ANTELACION_MAXIMA). Cada módulo lanza estas excepciones con su código.
 */
package com.tenisahora.shared.exception;
