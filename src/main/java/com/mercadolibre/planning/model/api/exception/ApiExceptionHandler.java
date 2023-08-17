package com.mercadolibre.planning.model.api.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import com.mercadolibre.fbm.wms.outbound.commons.web.response.ErrorResponse;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

  public static final String EXCEPTION_ATTRIBUTE = "application.exception";

  private static final String MISSING_PARAMETER = "missing_parameter";

  private static final String INVALID_ENTITY_TYPE = "invalid_entity_type";

  private static final String INVALID_PROJECTION_TYPE = "invalid_projection_type";

  private static final String BIND_ERROR = "bind_error";

  private static final String PROJECTION_TYPE_NOT_SUPPORTED = "projection_type_not_supported";

  private static final String ENTITY_ALREADY_EXIST = "entity_already_exists";

  private static final String ENTITY_NOT_FOUND = "entity_not_found";

  private static final String FORECAST_NOT_FOUND = "forecast_not_found";

  private static final String INVALID_DOMAIN_FILTER = "invalid_domain_filter";

  private static final String UNKNOWN_ERROR = "unknown_error";

  private static final String INVALID_FORECAST = "invalid_forecast";

  private static final String INVALID_DATE_RANGE = "invalid_date_range";

  private static final String ENTITY_TYPE_NOT_SUPPORTED = "entity_type_not_supported";

  @ExceptionHandler(BindException.class)
  public ResponseEntity<ErrorResponse> handleBindException(final BindException exception,
                                                           final HttpServletRequest request) {

    final ErrorResponse errorResponse = new ErrorResponse(
        BAD_REQUEST,
        exception.getMessage(),
        MISSING_PARAMETER
    );

    request.setAttribute(EXCEPTION_ATTRIBUTE, exception);
    log.error(BIND_ERROR, exception);
    return new ResponseEntity<>(errorResponse, new HttpHeaders(), errorResponse.getStatus());
  }

  @ExceptionHandler(InvalidEntityTypeException.class)
  public ResponseEntity<ErrorResponse> handleInvalidEntityTypeException(
      final InvalidEntityTypeException exception, final HttpServletRequest request) {

    final ErrorResponse errorResponse = new ErrorResponse(BAD_REQUEST,
        exception.getMessage(), INVALID_ENTITY_TYPE);

    request.setAttribute(EXCEPTION_ATTRIBUTE, exception);
    log.error(INVALID_ENTITY_TYPE, exception);
    return new ResponseEntity<>(errorResponse, new HttpHeaders(), errorResponse.getStatus());
  }

  @ExceptionHandler(InvalidProjectionTypeException.class)
  public ResponseEntity<ErrorResponse> handleInvalidProjectionTypeException(
      final InvalidProjectionTypeException exception, final HttpServletRequest request) {

    final ErrorResponse errorResponse = new ErrorResponse(BAD_REQUEST,
        exception.getMessage(), INVALID_PROJECTION_TYPE);

    request.setAttribute(EXCEPTION_ATTRIBUTE, exception);
    log.error(INVALID_PROJECTION_TYPE, exception);
    return new ResponseEntity<>(errorResponse, new HttpHeaders(), errorResponse.getStatus());
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleEntityNotFoundException(
      final EntityNotFoundException exception,
      final HttpServletRequest request) {

    final ErrorResponse errorResponse = new ErrorResponse(NOT_FOUND,
        exception.getMessage(), ENTITY_NOT_FOUND);

    request.setAttribute(EXCEPTION_ATTRIBUTE, exception);
    log.error(ENTITY_NOT_FOUND, exception);
    return new ResponseEntity<>(errorResponse, new HttpHeaders(), errorResponse.getStatus());
  }

  @ExceptionHandler(EntityTypeNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleEntityTypeNotSupportedException(
      final EntityTypeNotSupportedException exception, final HttpServletRequest request) {

    final ErrorResponse errorResponse = new ErrorResponse(BAD_REQUEST,
        exception.getMessage(), ENTITY_TYPE_NOT_SUPPORTED);

    request.setAttribute(EXCEPTION_ATTRIBUTE, exception);
    log.error(ENTITY_TYPE_NOT_SUPPORTED, exception);
    return new ResponseEntity<>(errorResponse, new HttpHeaders(), errorResponse.getStatus());
  }

  @ExceptionHandler(ProjectionTypeNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleProjectionTypeNotSupportedException(
      final ProjectionTypeNotSupportedException exception, final HttpServletRequest request) {

    final ErrorResponse errorResponse = new ErrorResponse(BAD_REQUEST,
        exception.getMessage(), PROJECTION_TYPE_NOT_SUPPORTED);

    request.setAttribute(EXCEPTION_ATTRIBUTE, exception);
    log.error(PROJECTION_TYPE_NOT_SUPPORTED, exception);
    return new ResponseEntity<>(errorResponse, new HttpHeaders(), errorResponse.getStatus());
  }

  @ExceptionHandler(EntityAlreadyExistsException.class)
  public ResponseEntity<ErrorResponse> handleEntityAlreadyExistsException(
      final EntityAlreadyExistsException exception, final HttpServletRequest request) {

    final ErrorResponse errorResponse = new ErrorResponse(BAD_REQUEST,
        exception.getMessage(), ENTITY_ALREADY_EXIST);

    request.setAttribute(EXCEPTION_ATTRIBUTE, exception);
    log.error(ENTITY_ALREADY_EXIST, exception);
    return new ResponseEntity<>(errorResponse, new HttpHeaders(), errorResponse.getStatus());
  }

  @ExceptionHandler(ForecastNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleForecastNotFoundException(
      final ForecastNotFoundException exception,
      final HttpServletRequest request) {

    final ErrorResponse errorResponse = new ErrorResponse(NOT_FOUND,
        exception.getMessage(), FORECAST_NOT_FOUND);

    request.setAttribute(EXCEPTION_ATTRIBUTE, exception);
    log.error(FORECAST_NOT_FOUND, exception);
    return new ResponseEntity<>(errorResponse, new HttpHeaders(), errorResponse.getStatus());
  }

  @ExceptionHandler(InvalidForecastException.class)
  public ResponseEntity<ErrorResponse> handleInvalidForecastException(
      final InvalidForecastException exception,
      final HttpServletRequest request) {

    final ErrorResponse errorResponse = new ErrorResponse(CONFLICT,
        exception.getMessage(), INVALID_FORECAST);

    request.setAttribute(EXCEPTION_ATTRIBUTE, exception);
    log.error(INVALID_FORECAST, exception);
    return new ResponseEntity<>(errorResponse, new HttpHeaders(), errorResponse.getStatus());
  }

  @ExceptionHandler(InvalidInputFilterException.class)
  public ResponseEntity<ErrorResponse> handleInvalidDomainFilter(final InvalidInputFilterException exception,
                                                                 final HttpServletRequest request) {

    final ErrorResponse errorResponse = new ErrorResponse(BAD_REQUEST,
        exception.getMessage(), INVALID_DOMAIN_FILTER);
    request.setAttribute(EXCEPTION_ATTRIBUTE, exception);
    log.error(INVALID_DOMAIN_FILTER);
    return new ResponseEntity<>(errorResponse, new HttpHeaders(), errorResponse.getStatus());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(
      final Exception exception,
      final HttpServletRequest request) {

    final ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        exception.getMessage(),
        UNKNOWN_ERROR);

    request.setAttribute(EXCEPTION_ATTRIBUTE, exception);

    log.error(UNKNOWN_ERROR, exception);
    return new ResponseEntity<>(errorResponse, new HttpHeaders(), errorResponse.getStatus());
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingParameterException(
      final MissingServletRequestParameterException exception,
      final HttpServletRequest request) {

    final ErrorResponse errorResponse = new ErrorResponse(
        BAD_REQUEST,
        exception.getMessage(),
        MISSING_PARAMETER
    );

    request.setAttribute(EXCEPTION_ATTRIBUTE, exception);
    log.error(exception.getMessage(), exception);
    return new ResponseEntity<>(errorResponse, new HttpHeaders(), errorResponse.getStatus());
  }

  @ExceptionHandler({InvalidDateRangeException.class, DateRangeException.class})
  public ResponseEntity<ErrorResponse> handleInvalidDateRangeException(
      final Exception exception,
      final HttpServletRequest request) {

    final ErrorResponse errorResponse = new ErrorResponse(
        BAD_REQUEST,
        exception.getMessage(),
        INVALID_DATE_RANGE
    );

    request.setAttribute(EXCEPTION_ATTRIBUTE, exception);
    log.error(exception.getMessage(), exception);
    return new ResponseEntity<>(errorResponse, new HttpHeaders(), errorResponse.getStatus());
  }

  @ExceptionHandler({BadRequestException.class, HttpMessageNotReadableException.class})
  public ResponseEntity<ErrorResponse> handleBadRequestException(
      final Exception exception,
      final HttpServletRequest request) {

    final ErrorResponse errorResponse = new ErrorResponse(
        BAD_REQUEST,
        exception.getMessage(),
        exception.getMessage()
    );

    request.setAttribute(EXCEPTION_ATTRIBUTE, exception);
    log.error(exception.getMessage(), exception);
    return new ResponseEntity<>(errorResponse, new HttpHeaders(), errorResponse.getStatus());
  }
}
