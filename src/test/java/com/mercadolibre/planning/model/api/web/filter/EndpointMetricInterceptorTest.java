package com.mercadolibre.planning.model.api.web.filter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
public class EndpointMetricInterceptorTest {

    private static EndpointMetricInterceptor interceptor;

    @BeforeAll
    public static void setup() {
        interceptor = new EndpointMetricInterceptor();
    }

    @Test
    void testGetClientNameWithName() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(EndpointMetricInterceptor.REQUEST_HEADER_CLIENT_APPLICATION, "client-x");

        String actual = interceptor.getClientName(request);
        Assertions.assertEquals("client-x", actual);
    }

    @Test
    void testGetClientNameWithoutName() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        // do not set client header

        String actual = interceptor.getClientName(request);
        Assertions.assertEquals(EndpointMetricInterceptor.DEFAULT_CLIENT, actual);
    }

    @Test
    void testGetCleanPathReplacePattern() {
        String raw = "/logistic_center/{logisticCenterId}/ratios/packing_wall";
        String expected = "/logistic_center/logistic_center_id/ratios/packing_wall";

        String actual = interceptor.getCleanPath(raw);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void preHandlerShouldNotFailWithNullPath() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        boolean response = interceptor.preHandle(request, null, null);
        Assertions.assertEquals(true, response);
    }
}
