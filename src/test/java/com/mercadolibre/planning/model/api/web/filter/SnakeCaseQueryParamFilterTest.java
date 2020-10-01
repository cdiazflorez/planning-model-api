package com.mercadolibre.planning.model.api.web.filter;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SnakeCaseQueryParamFilterTest {

    @Spy
    private SnakeCaseQueryParamFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Captor
    private ArgumentCaptor<HttpServletRequestWrapper> requestWrapperArgumentCaptor;

    @ParameterizedTest
    @MethodSource("underscoreQueryParams")
    public void testFilter(final Map<String, String[]> underscoreQueryParams,
                           final Map<String, String[]> camelCaseQueryParams)
            throws ServletException, IOException {
        // GIVEN
        when(request.getParameterMap()).thenReturn(underscoreQueryParams);

        // WHEN
        filter.doFilterInternal(request, response, filterChain);

        // THEN
        verify(filterChain).doFilter(requestWrapperArgumentCaptor.capture(), eq(response));
        final HttpServletRequestWrapper requestWrapper = requestWrapperArgumentCaptor.getValue();
        assertEquals(camelCaseQueryParams.keySet(), requestWrapper.getParameterMap().keySet());
    }

    private static Stream<Arguments> underscoreQueryParams() {
        return Stream.of(
                Arguments.of(
                        Map.of(),
                        Map.of()),
                Arguments.of(
                        Map.of("warehouse_id", new String[]{"BRSP01"}),
                        Map.of("warehouseId", new String[]{"BRSP01"})),
                Arguments.of(
                        Map.of("warehouse_id", new String[]{"ARBA01"},
                                "date_to", new String[]{"2020-08-19T13:00:00Z"}),
                        Map.of("warehouseId", new String[]{"ARBA01"},
                                "dateTo", new String[]{"2020-08-19T13:00:00Z"}))
        );
    }
}
