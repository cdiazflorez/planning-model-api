package com.mercadolibre.planning.model.api;

import com.mercadolibre.planning.model.api.web.controller.PingController;
import com.mercadolibre.restclient.MockResponse;
import com.mercadolibre.restclient.http.ContentType;
import com.mercadolibre.restclient.mock.RequestMockHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;

import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static com.mercadolibre.restclient.http.ContentType.HEADER_NAME;
import static com.mercadolibre.restclient.http.HttpMethod.GET;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.HttpStatus.OK;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("functional")
@ActiveProfiles("development")
public class PlanningModelApplicationTest {

    private static final String FEED_DEFERRAL_URL = "/feed/deferral";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String REQUEST_ID_HEADER_VALUE = "e092a8f9-bf55-4668-b91c-9318a0669c35";

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PingController pingController;

    @BeforeEach
    public void setUp() {
        RequestMockHolder.clear();
    }

    @Test
    @DisplayName("Context Loads")
    public void contextLoads() {
        assertNotNull(pingController);
    }

    @Test
    @DisplayName("With restrictive matching of melicontext headers in flow client "
            + "then response is ok")
    public void testMeliContextHeadersTrace() throws Exception {
        // GIVEN
        final String request = getResourceAsString("deferralBQMessage.json");
        final HttpHeaders headers = givenMeliContextHeaders();

        givenDeferralStatusSuccess();

        // WHEN
        final ResponseEntity<String> response =
                restTemplate.postForEntity(
                        baseUrl(port) + FEED_DEFERRAL_URL,
                        new HttpEntity<>(request, headers),
                        String.class
                );

        // THEN
        assertEquals(OK, response.getStatusCode());
        assertEquals("Message processed for warehouseId: ARBA01", response.getBody());
    }

    private HttpHeaders givenMeliContextHeaders() {
        final HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        headers.set(REQUEST_ID_HEADER, REQUEST_ID_HEADER_VALUE);

        return headers;
    }

    private void givenDeferralStatusSuccess() throws IOException {
        MockResponse.builder()
                .withMethod(GET)
                .withURL("https://internal-api.mercadolibre.com/fbm/flow-monitor-stage"
                        + format("/warehouses/%s/workflows/%s" + "/projections",
                        WAREHOUSE_ID,
                        FBM_WMS_OUTBOUND))
                .withStatusCode(OK.value())
                .withResponseHeader(HEADER_NAME, ContentType.APPLICATION_JSON.toString())
                .withRequestHeader(REQUEST_ID_HEADER, REQUEST_ID_HEADER_VALUE)
                .withRequestHeader("x-socket-timeout", "1000")
                .withRequestHeader("x-rest-pool-name", "FLOW_MONITOR")
                .withRestrictiveMatching(true)
                .withResponseBody(
                        getResourceAsString("get_flow_monitor_response.json"))
                .build();
    }

    private static String baseUrl(final int port) {
        return format("http://localhost:%s", port);
    }
}
