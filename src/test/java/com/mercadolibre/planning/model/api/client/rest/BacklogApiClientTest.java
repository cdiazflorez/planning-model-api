package com.mercadolibre.planning.model.api.client.rest;

import com.mercadolibre.fbm.wms.outbound.commons.rest.exception.ClientException;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.BacklogPhoto;
import com.mercadolibre.restclient.MockResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.restclient.http.ContentType.APPLICATION_JSON;
import static com.mercadolibre.restclient.http.ContentType.HEADER_NAME;
import static com.mercadolibre.restclient.http.HttpMethod.GET;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

class BacklogApiClientTest extends BaseClientTest {
    private static final String URL = "/backlogs/logistic_centers/%s/backlogs/current";

    private static final String QUERY = "?workflows=inbound&"
            + "processes=check_in&"
            + "sla_from=2021-08-12T01:00:00Z&"
            + "sla_to=2021-08-12T05:00:00Z&"
            + "group_by=step";

    private static final Instant DATE_FROM = Instant.parse("2021-08-12T01:00:00Z");

    private static final Instant DATE_TO = Instant.parse("2021-08-12T05:00:00Z");

    private BacklogApiClient client;

    @BeforeEach
    public void setUp() throws Exception {
        client = new BacklogApiClient(getRestTestClient());
    }

    @AfterEach
    public void tearDown() {
        cleanMocks();
    }

    @Test
    public void testGetBacklogOK() throws JSONException {
        // GIVEN
        mockSuccessfulResponse();

        // WHEN
        final List<BacklogPhoto> result = client.getCurrentBacklog(
                WAREHOUSE_ID,
                of(FBM_WMS_INBOUND),
                of("check_in"),
                DATE_FROM,
                DATE_TO,
                of("step")
        );

        // THEN
        assertEquals(3, result.size());

        final BacklogPhoto firstBacklogPhoto = result.get(0);
        assertEquals(1255, firstBacklogPhoto.getTotal());
        assertEquals(DATE_FROM, firstBacklogPhoto.getDate());

        final Map<String, String> keys = firstBacklogPhoto.getKeys();
        assertEquals("2021-01-01T00:00", keys.get("date_in"));
        assertEquals("2021-01-02T00:00", keys.get("date_out"));
    }

    @Test
    public void testGetBacklogErr() {
        // GIVEN
        mockErroneousResponse();

        // WHEN
        assertThrows(ClientException.class, () ->
                client.getCurrentBacklog(
                        WAREHOUSE_ID,
                        of(FBM_WMS_INBOUND),
                        of("check_in"),
                        DATE_FROM,
                        DATE_TO,
                        of("date_in", "date_out")
                )
        );
    }

    private void mockSuccessfulResponse() throws JSONException {
        final String url = String.format(URL, WAREHOUSE_ID) + QUERY;

        MockResponse.builder()
                .withMethod(GET)
                .withURL(BASE_URL_ROUTE + url)
                .withStatusCode(OK.value())
                .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
                .withResponseBody(buildResponse().toString())
                .build();
    }

    private JSONArray buildResponse() throws JSONException {
        final Map<String, String> mockedValues = Map.of(
                "date_in", "2021-01-01T00:00",
                "date_out", "2021-01-02T00:00");

        return new JSONArray()
                .put(
                        new JSONObject()
                                .put("date", "2021-08-12T01:00:00Z")
                                .put("total", 1255)
                                .put("keys", new JSONObject(mockedValues)))
                .put(
                        new JSONObject()
                                .put("date", "2021-08-12T02:00:00Z")
                                .put("total", 255)
                                .put("keys", new JSONObject(mockedValues)))
                .put(
                        new JSONObject()
                                .put("date", "2021-08-12T03:00:00Z")
                                .put("total", 300)
                                .put("keys", new JSONObject(mockedValues)));
    }

    private void mockErroneousResponse() {
        final String url = String.format(URL, WAREHOUSE_ID) + QUERY;

        MockResponse.builder()
                .withMethod(GET)
                .withURL(BASE_URL_ROUTE + url)
                .withStatusCode(INTERNAL_SERVER_ERROR.value())
                .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
                .build();
    }
}

