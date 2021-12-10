package com.mercadolibre.planning.model.api.client.rest;

import com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets.DayDto;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets.RouteEtsDto;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets.RouteEtsRequest;
import com.mercadolibre.restclient.MockResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.mercadolibre.restclient.http.ContentType.APPLICATION_JSON;
import static com.mercadolibre.restclient.http.ContentType.HEADER_NAME;
import static com.mercadolibre.restclient.http.HttpMethod.POST;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RouteEtsClientTest  extends BaseClientTest {

    private static final String URL = "/shipping/route/multisearch/estimated-times";

    private RouteEtsClient client;

    private static final Map<Integer, String> DAYS = Map.of(
            0, "monday",
            1,"tuesday",
            2,"wednesday",
            3,"thursday",
            4,"friday"
    );

    @BeforeEach
    public void setUp() throws IOException {
        client =  new RouteEtsClient(getRestTestClient(), objectMapper());
    }

    @AfterEach
    public void tearDown() {
        super.cleanMocks();
    }


    @Test
    public void testGetProjection() {
        // GIVEN
        final RouteEtsRequest request = RouteEtsRequest.builder()
                .fromFilter(List.of(new String[]{"ARBA01"}))
                .build();

        MockResponse.builder()
                .withMethod(POST)
                .withURL(BASE_URL_ROUTE + URL)
                .withRequestBody(getResourceAsString("post_route_ets_request.json"))
                .withResponseBody(getResourceAsString("post_route_ets_response.json"))
                .withStatusCode(HttpStatus.OK.value())
                .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
                .build();

        final List<RouteEtsDto> response = client.postRoutEts(request);

        //TEST
        for (final RouteEtsDto routeEts: response) {

            assertEquals(routeEts.getId(),"ARBA01_SCF1_158662");
            assertEquals(routeEts.getFrom(), "ARBA01");
            assertEquals(routeEts.getCanalization(), "SCF1");
            assertEquals(routeEts.getServiceId(), "158662");

            testWeek(generateWeek(routeEts.getFixedEtsByDay()));

        }
    }

    private Map<Integer, DayDto> generateWeek(final Map<String, List<DayDto>> days) {
        return Map.of(
                0, days.get("monday").get(0),
                1, days.get("tuesday").get(0),
                2, days.get("wednesday").get(0),
                3, days.get("thursday").get(0),
                4, days.get("friday").get(0)
        );
    }

    private void testWeek(final Map<Integer, DayDto> week) {
        for (int i = 0; i < DAYS.size(); i++) {
            assertEquals(week.get(i).getEtDay(), DAYS.get(i));
            assertEquals(week.get(i).getEtHour(), "0200");
            assertEquals(week.get(i).getProcessingTime(), "0400");
            assertEquals(week.get(i).getType(), "cpt");
            assertFalse(week.get(i).isShouldDeferral());
        }
    }

}
