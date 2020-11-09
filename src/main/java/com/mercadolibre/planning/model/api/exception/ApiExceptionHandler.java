package com.mercadolibre.planning.model.api.exception;

import com.mercadolibre.fbm.wms.outbound.commons.web.response.ErrorResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestControllerAdvice
public class ApiExceptionHandler {

    public static final String EXCEPTION_ATTRIBUTE = "application.exception";

    @ExceptionHandler(InvalidEntityTypeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidEntityTypeException(
            final InvalidEntityTypeException exception, final HttpServletRequest request) {

        final ErrorResponse errorResponse = new ErrorResponse(BAD_REQUEST,
                exception.getMessage(), "invalid_entity_type");

        request.setAttribute(EXCEPTION_ATTRIBUTE, exception);
        return new ResponseEntity<>(errorResponse, new HttpHeaders(), errorResponse.getStatus());
    }

    @ExceptionHandler(InvalidProjectionTypeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidProjectionTypeException(
            final InvalidProjectionTypeException exception, final HttpServletRequest request) {

        final ErrorResponse errorResponse = new ErrorResponse(BAD_REQUEST,
                exception.getMessage(), "invalid_projection_type");

        request.setAttribute(EXCEPTION_ATTRIBUTE, exception);
        return new ResponseEntity<>(errorResponse, new HttpHeaders(), errorResponse.getStatus());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handle(final EntityNotFoundException exception,
                                                final HttpServletRequest request) {

        final ErrorResponse errorResponse = new ErrorResponse(NOT_FOUND,
                exception.getMessage(), "entity_not_found");

        request.setAttribute(EXCEPTION_ATTRIBUTE, exception);
        return new ResponseEntity<>(errorResponse, new HttpHeaders(), errorResponse.getStatus());
    }

    @ExceptionHandler(EntityTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleEntityTypeNotSupportedException(
            final EntityTypeNotSupportedException exception, final HttpServletRequest request) {

        final ErrorResponse errorResponse = new ErrorResponse(BAD_REQUEST,
                exception.getMessage(), "entity_type_not_supported");

        request.setAttribute(EXCEPTION_ATTRIBUTE, exception);
        return new ResponseEntity<>(errorResponse, new HttpHeaders(), errorResponse.getStatus());
    }

    @ExceptionHandler(ProjectionTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleProjectionTypeNotSupportedException(
            final ProjectionTypeNotSupportedException exception, final HttpServletRequest request) {

        final ErrorResponse errorResponse = new ErrorResponse(BAD_REQUEST,
                exception.getMessage(), "projection_type_not_supported");

        request.setAttribute(EXCEPTION_ATTRIBUTE, exception);
        return new ResponseEntity<>(errorResponse, new HttpHeaders(), errorResponse.getStatus());
    }

    @ExceptionHandler(EntityAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEntityAlreadyExistsException(
            final EntityAlreadyExistsException exception, final HttpServletRequest request) {

        final ErrorResponse errorResponse = new ErrorResponse(BAD_REQUEST,
                exception.getMessage(), "entity_already_exists");

        request.setAttribute(EXCEPTION_ATTRIBUTE, exception);
        return new ResponseEntity<>(errorResponse, new HttpHeaders(), errorResponse.getStatus());
    }

}
