package com.mercadolibre.planning.model.api.logging;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Slf4j
@Component
public class ApiLoggingFilter implements Filter {

  private static final Set<String> METHOD_NAMES = Set.of("GET", "POST", "PUT");

  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException,
      ServletException {
    ContentCachingRequestWrapper cachedRequest = this.cachedRequest(servletRequest);
    ContentCachingResponseWrapper cachedResponse = this.cachedResponse(servletResponse);

    try {
      this.preHandle(cachedRequest);
      filterChain.doFilter(cachedRequest, cachedResponse);
    } finally {
      this.postHandle(cachedRequest, cachedResponse);
      cachedResponse.copyBodyToResponse();
      MDC.clear();
    }

  }

  private ContentCachingRequestWrapper cachedRequest(ServletRequest servletRequest) {
    return new ContentCachingRequestWrapper((HttpServletRequest) servletRequest);
  }

  private ContentCachingResponseWrapper cachedResponse(ServletResponse servletResponse) {
    return new ContentCachingResponseWrapper((HttpServletResponse) servletResponse);
  }

  private void preHandle(HttpServletRequest request) {
    MDC.put("request_id", UUID.randomUUID().toString());
    MDC.put("request_uri", request.getRequestURI());
    MDC.put("request_method", request.getMethod());
  }

  private void postHandle(final ContentCachingRequestWrapper request, final ContentCachingResponseWrapper response) {
    final Exception exception = this.getRequestException(request);
    final String message = LogMessageBuilder.builder().include(request).include(response).include(exception).build();
    logMessage(message, exception, response, request);
  }

  private void logMessage(
      final String message,
      final Throwable exception,
      final HttpServletResponse response,
      final HttpServletRequest request
  ) {
    if (shouldLogError(response)) {
      getLog().error(message, exception);
    } else if (shouldLogWarn(response)) {
      getLog().warn(message, exception);
    } else if (shouldLogInfo(request)) {
      getLog().info(message);
    } else if (shouldLogDebug()) {
      getLog().debug(message);
    }
  }

  private Exception getRequestException(HttpServletRequest request) {
    return (Exception) request.getAttribute("application.exception");
  }

  private Logger getLog() {
    return log;
  }

  private boolean shouldLogError(final HttpServletResponse httpServletResponse) {
    return isServerError(httpServletResponse.getStatus());
  }

  private boolean shouldLogWarn(final HttpServletResponse httpServletResponse) {
    return isClientError(httpServletResponse.getStatus());
  }

  private boolean shouldLogInfo(final HttpServletRequest httpServletRequest) {
    String uri = httpServletRequest.getRequestURI();

    if (uri != null && uri.equals("/ping")) {
      return false;
    }
    return METHOD_NAMES.contains(httpServletRequest.getMethod().toUpperCase(Locale.US));
  }

  private boolean shouldLogDebug() {
    return false;
  }

  private boolean isServerError(final int status) {
    return isStatus(status, HttpStatus::is5xxServerError);
  }

  private boolean isClientError(final int status) {
    return isStatus(status, HttpStatus::is4xxClientError);
  }

  private boolean isStatus(final int responseStatus, final Predicate<HttpStatus> mapper) {

    final HttpStatus httpStatus = HttpStatus.resolve(responseStatus);
    return httpStatus != null && mapper.test(httpStatus);
  }
}
