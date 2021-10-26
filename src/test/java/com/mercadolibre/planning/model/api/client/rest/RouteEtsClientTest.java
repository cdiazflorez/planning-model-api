package com.mercadolibre.planning.model.api.client.rest;

import com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets.DayDto;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets.FixedEtsByDayDto;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets.RouteEtsDto;
import com.mercadolibre.restclient.MockResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.mercadolibre.restclient.http.ContentType.APPLICATION_JSON;
import static com.mercadolibre.restclient.http.ContentType.HEADER_NAME;
import static com.mercadolibre.restclient.http.HttpMethod.GET;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.http.HttpStatus.OK;

public class RouteEtsClientTest  extends BaseClientTest {

    private static final String URL = "/multisearch/estimated-times?from=%s";

    private static final String WAREHOUSE_ID = "ARBA01";

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
        MockResponse.builder()
                .withMethod(GET)
                .withURL(BASE_URL_ROUTE + format(URL, WAREHOUSE_ID))
                .withStatusCode(OK.value())
                .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
                .withResponseBody(
                        getResourceAsString("get_route_ets_response.json"))
                .build();

        final List<RouteEtsDto> response = client.getRoutEts(WAREHOUSE_ID);

        //TEST
        for (final RouteEtsDto routeEts: response) {

            assertEquals(routeEts.getId(),"ARBA01_SCF1_158662");
            assertEquals(routeEts.getFrom(), "ARBA01");
            assertEquals(routeEts.getCanalization(), "SCF1");
            assertEquals(routeEts.getServiceId(), "158662");

            testWeek(generateWeek(routeEts.getFixedEtsByDay()));

        }


    }

    private Map<Integer, DayDto> generateWeek(final FixedEtsByDayDto fixedEtsByDayDto) {
        return Map.of(
                0, fixedEtsByDayDto.getMonday().get(0),
                1, fixedEtsByDayDto.getTuesday().get(0),
                2, fixedEtsByDayDto.getWednesday().get(0),
                3, fixedEtsByDayDto.getThursday().get(0),
                4, fixedEtsByDayDto.getFriday().get(0)
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
