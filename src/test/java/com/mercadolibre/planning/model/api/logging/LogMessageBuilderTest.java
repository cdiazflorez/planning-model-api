package com.mercadolibre.planning.model.api.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

class LogMessageBuilderTest {

  private static final String REQUEST_CONTENT = "This is the request content";

  private static final String REQUEST_HEADER = "request_header";

  private static final String REQUEST_URI = "/example";

  private static final String REQUEST_QUERY = "params=param";

  private static final String RESPONSE = "This is the response";

  private static final String RESPONSE_HEADER = "response_header";

  private static final String HEADER_VALUE = "header_value";

  private static final String METHOD = "METHOD";

  private static final String EXCEPTION_MESSAGE = "This is the Exception message";

  private void configureRequest(ContentCachingRequestWrapper request) {
    when(request.getContentAsByteArray()).thenReturn(REQUEST_CONTENT.getBytes());
    when(request.getHeaderNames()).thenReturn(Collections.enumeration(List.of(REQUEST_HEADER)));
    when(request.getHeaders(REQUEST_HEADER)).thenReturn(Collections.enumeration(List.of(HEADER_VALUE)));
    when(request.getMethod()).thenReturn(METHOD);
    when(request.getRequestURI()).thenReturn(REQUEST_URI);
    when(request.getQueryString()).thenReturn(REQUEST_QUERY);
  }

  private void configureResponse(ContentCachingResponseWrapper response) {
    when(response.getContentAsByteArray()).thenReturn(RESPONSE.getBytes());
    when(response.getHeaderNames()).thenReturn(List.of(RESPONSE_HEADER));
    when(response.getHeaders(RESPONSE_HEADER)).thenReturn(List.of(HEADER_VALUE));
  }


  @Test
  void testLogMessage() {
    //GIVEN
    var request = Mockito.mock(ContentCachingRequestWrapper.class);
    configureRequest(request);
    var response = Mockito.mock(ContentCachingResponseWrapper.class);
    configureResponse(response);
    when(response.getStatus()).thenReturn(HttpStatus.OK.value());

    //WHEN
    var log = LogMessageBuilder.builder().include(request).include(response).build();

    //THEN
    assertEquals(
        """
            Request processed successfully\s
            	Request:
            		Method = METHOD
            		URI = /example
            		Query = params=param
            		Headers = {request_header=[header_value]}
            		Body = This is the request content\s
            	Response:
            		Status = 200
            		Headers = {response_header=[header_value]}
            """,
        log
    );
  }

  @Test
  void testLogMessageException() {
    //GIVEN
    var request = Mockito.mock(ContentCachingRequestWrapper.class);
    configureRequest(request);

    var response = Mockito.mock(ContentCachingResponseWrapper.class);
    configureResponse(response);
    when(response.getStatus()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR.value());

    var exception = Mockito.mock(Exception.class);
    when(exception.getMessage()).thenReturn(EXCEPTION_MESSAGE);

    //WHEN
    var log = LogMessageBuilder.builder().include(request).include(response).include(exception).build();

    //THEN
    assertEquals(
        """
            This is the Exception message\s
            	Request:
            		Method = METHOD
            		URI = /example
            		Query = params=param
            		Headers = {request_header=[header_value]}
            		Body = This is the request content\s
            	Response:
            		Status = 500
            		Headers = {response_header=[header_value]}
            		Body = This is the response
            """,
        log
    );
  }
}
