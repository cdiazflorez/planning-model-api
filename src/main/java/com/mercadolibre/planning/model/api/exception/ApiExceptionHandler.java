package com.mercadolibre.planning.model.api.exception;

import com.mercadolibre.fbm.wms.outbound.commons.web.response.ErrorResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestControllerAdvice
public class ApiExceptionHandler {

    public static final String EXCEPTION_ATTRIBUTE = "application.exception";

    @ExceptionHandler(InvalidEntityTypeException.class)
    public ResponseEntity<ErrorResponse> handle(final InvalidEntityTypeException exception,
                                                final HttpServletRequest request) {

        final ErrorResponse errorResponse = new ErrorResponse(BAD_REQUEST,
                exception.getMessage(), "invalid_entity_type");

        request.setAttribute(EXCEPTION_ATTRIBUTE, exception);
        return new ResponseEntity<>(errorResponse, new HttpHeaders(), errorResponse.getStatus());
    }
}
