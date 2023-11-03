package com.mercadolibre.planning.model.api.logging;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

public final class LogMessageBuilder {
  private String message = "Request processed successfully";

  private String exception = "";

  private String request = "";

  private String response = "";

  private String delimiter = ",";

  private LogMessageBuilder() {
  }

  public static LogMessageBuilder builder() {
    return new LogMessageBuilder();
  }

  public LogMessageBuilder include(Exception exception) {
    if (exception != null) {
      this.exception = exception.getMessage();
      this.message = "";
    }

    return this;
  }

  public LogMessageBuilder include(ContentCachingRequestWrapper request) {
    String content = this.formatBody(request.getContentAsByteArray());
    Map<String, Collection<String>> headers = this.getHeadersByName(request);
    this.request = this.formatRequest(request, content, headers);
    return this;
  }

  public LogMessageBuilder include(ContentCachingResponseWrapper response) {
    String content = this.formatBody(response.getContentAsByteArray());
    Map<String, Collection<String>> headers = this.getHeadersByName(response);
    this.response = this.formatResponse(response, content, headers);
    return this;
  }

  public String build() {
    return Stream.of(this.message, this.exception, this.request, this.response).filter(this::isNotBlank)
        .collect(Collectors.joining(" "));
  }

  private String formatRequest(ContentCachingRequestWrapper request, String content, Map<String, Collection<String>> headers) {
    return String.format("%n\tRequest:%n\t\tMethod = %s%n\t\tURI = %s%n\t\tQuery = %s%n\t\tHeaders = %s%n\t\tBody = %s",
        request.getMethod(), request.getRequestURI(), this.emptyIfBlank(request.getQueryString()), this.formatHeaders(headers),
        this.emptyIfBlank(content));
  }

  private String formatResponse(ContentCachingResponseWrapper response, String content, Map<String, Collection<String>> headers) {

    return HttpStatus.valueOf(response.getStatus()).is2xxSuccessful()
        ? String.format("%n\tResponse:%n\t\tStatus = %s%n\t\tHeaders = %s%n", response.getStatus(), this.formatHeaders(headers))
        : String.format("%n\tResponse:%n\t\tStatus = %s%n\t\tHeaders = %s%n\t\tBody = %s%n",
        response.getStatus(), this.formatHeaders(headers), this.emptyIfBlank(content));
  }

  private String formatBody(byte[] bodyBytes) {
    String content = new String(bodyBytes, StandardCharsets.UTF_8);
    return this.isNotBlank(content) ? this.minify(content) : content;
  }

  private String emptyIfBlank(String value) {
    return this.isNotBlank(value) ? value : "{}";
  }

  private boolean isNotBlank(String value) {
    return value != null && !value.isBlank();
  }

  private String minify(String content) {
    return content;
  }

  private String formatHeaders(Map<String, Collection<String>> headersByName) {
    return headersByName.entrySet().stream().map(this::formatHeader).collect(Collectors.joining(delimiter, "{", "}"));
  }

  private String formatHeader(Map.Entry<String, Collection<String>> entry) {
    return String.format("%s=[%s]", entry.getKey(), String.join(delimiter, entry.getValue()));
  }

  private Map<String, Collection<String>> getHeadersByName(HttpServletRequest request) {
    return this.getHeadersByName(
        Collections.list(request.getHeaderNames()),
        headerName -> Collections.list(request.getHeaders(headerName))
    );
  }

  private Map<String, Collection<String>> getHeadersByName(HttpServletResponse response) {
    return this.getHeadersByName(
        response.getHeaderNames(),
        headerName -> new ArrayList<>(response.getHeaders(headerName))
    );
  }

  private Map<String, Collection<String>> getHeadersByName(Collection<String> headerNames, Function<String, Collection<String>> mapper) {
    return headerNames.stream().collect(Collectors.toMap(Function.identity(), mapper));
  }
}
