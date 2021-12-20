package com.mercadolibre.planning.model.api.client.rest;

import com.mercadolibre.planning.model.api.domain.usecase.cptbywarehouse.Canalization;
import com.mercadolibre.planning.model.api.domain.usecase.cptbywarehouse.CarrierServiceId;
import com.mercadolibre.planning.model.api.domain.usecase.cptbywarehouse.RouteCoverageResult;
import com.mercadolibre.restclient.MockResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.List;

import static com.mercadolibre.restclient.http.ContentType.APPLICATION_JSON;
import static com.mercadolibre.restclient.http.ContentType.HEADER_NAME;
import static com.mercadolibre.restclient.http.HttpMethod.GET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RouteCoveragePageClientTest extends BaseClientTest {

    private static final String URL = "/shipping/routes/rules?status=active&site=MLA&from=ARBA01";
    private static final String SCROLL_ID = "&scroll_id=c2FuZGJveC1hbHBoYQ";
    private static final long MAX_AGE = 60L * 60L * 1_000_000_000L;
    private long nanoTime;

    @AfterEach
    public void tearDown() {
        super.cleanMocks();
    }

    @Test
    public void testGet() throws IOException {

        final var client = new RouteCoverageClient(getRestTestClient(), new NanoTimeService() {
            @Override
            public long getNanoTime() {
                return nanoTime;
            }
        }, () -> Runnable::run);


        MockResponse.builder()
                .withMethod(GET)
                .withURL(BASE_URL_ROUTE + URL)
                .withResponseBody(getResourceAsString("get_route_coverage_response.json"))
                .withStatusCode(HttpStatus.OK.value())
                .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
                .build();

        MockResponse.builder()
                .withMethod(GET)
                .withURL(BASE_URL_ROUTE + URL + SCROLL_ID)
                .withResponseBody(getResourceAsString(
                        "get_route_coverage_response_result_null.json"))
                .withStatusCode(HttpStatus.OK.value())
                .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
                .build();


        final List<RouteCoverageResult> response = client.get("ARBA01");

        nanoTime += MAX_AGE;

        final List<RouteCoverageResult> responseCached = client.get("ARBA01");

        final List<RouteCoverageResult> expected = expected();

        assertEquals(expected, response);
        assertNotNull(responseCached);


    }

    private List<RouteCoverageResult> expected() {

        return List.of(new RouteCoverageResult(
                        new Canalization("1234",
                                List.of(
                                        new CarrierServiceId("831")
                                )),
                        "active"),

                new RouteCoverageResult(
                        new Canalization("12345",
                                List.of(
                                        new CarrierServiceId("158663"))),
                        "active"
                )

        );
    }


}
