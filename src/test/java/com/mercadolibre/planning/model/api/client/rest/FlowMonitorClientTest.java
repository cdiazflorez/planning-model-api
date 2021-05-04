package com.mercadolibre.planning.model.api.client.rest;

import com.mercadolibre.planning.model.api.domain.usecase.deferral.DeferralDto;
import com.mercadolibre.restclient.MockResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.restclient.http.ContentType.APPLICATION_JSON;
import static com.mercadolibre.restclient.http.ContentType.HEADER_NAME;
import static com.mercadolibre.restclient.http.HttpMethod.GET;
import static java.lang.String.format;
import static java.time.ZonedDateTime.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.OK;

public class FlowMonitorClientTest extends BaseClientTest {

    private static final String URL = "/fbm/flow/monitor/warehouses/%s/workflows/%s";

    private static final String WAREHOUSE_ID = "ARTW01";

    private FlowMonitorClient client;

    @BeforeEach
    public void setUp() throws IOException {
        client = new FlowMonitorClient(getRestTestClient(), objectMapper());
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
                .withURL(BASE_URL + format(URL + "/projections", WAREHOUSE_ID, FBM_WMS_OUTBOUND))
                .withStatusCode(OK.value())
                .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
                .withResponseBody(
                        getResourceAsString("get_flow_monitor_response.json"))
                .build();

        //WHEN
        final DeferralDto response = client.getDeferralProjection(WAREHOUSE_ID, FBM_WMS_OUTBOUND);

        //THEN
        assertEquals(WAREHOUSE_ID, response.getWarehouseId());
        assertEquals(FBM_WMS_OUTBOUND.name(), response.getWorkflow());
        assertEquals(3, response.getProjections().size());

        assertEquals(parse("2021-04-23T13:00:00Z"), response.getProjections().get(0)
                .getEstimatedTimeDeparture().withFixedOffsetZone());
        assertTrue(response.getProjections().get(0).isShouldDeferral());

        assertEquals(parse("2021-04-23T14:00:00Z"), response.getProjections().get(1)
                .getEstimatedTimeDeparture().withFixedOffsetZone());
        assertFalse(response.getProjections().get(1).isShouldDeferral());

        assertEquals(parse("2021-04-23T15:00:00Z"), response.getProjections().get(2)
                .getEstimatedTimeDeparture().withFixedOffsetZone());
        assertFalse(response.getProjections().get(2).isShouldDeferral());
    }
}
