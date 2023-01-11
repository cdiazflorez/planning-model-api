package com.mercadolibre.planning.model.api.client.rest;

import static com.mercadolibre.planning.model.api.domain.entity.BacklogGrouper.DATE_OUT;
import static com.mercadolibre.planning.model.api.domain.entity.BacklogGrouper.WORKFLOW;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.INBOUND_TRANSFER;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.restclient.http.ContentType.APPLICATION_JSON;
import static com.mercadolibre.restclient.http.ContentType.HEADER_NAME;
import static com.mercadolibre.restclient.http.HttpMethod.GET;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

import com.mercadolibre.fbm.wms.outbound.commons.rest.exception.ClientException;
import com.mercadolibre.planning.model.api.domain.entity.LastPhotoRequest;
import com.mercadolibre.planning.model.api.domain.entity.PhotoRequest;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.BacklogPhoto;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.Photo;
import com.mercadolibre.restclient.MockResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BacklogApiClientTest extends BaseClientTest {
  private static final String URL = "/backlogs/logistic_centers/%s/backlogs/current";
  private static final String PHOTO_URL = "/backlogs/logistic_centers/%s/photos";
  private static final String QUERY = "?workflows=inbound&"
      + "processes=check_in&"
      + "sla_from=2021-08-12T01:00:00Z&"
      + "sla_to=2021-08-12T05:00:00Z&"
      + "group_by=step";
  private static final Instant DATE_FROM = Instant.parse("2021-08-12T01:00:00Z");
  private static final Instant DATE_TO = Instant.parse("2021-08-12T05:00:00Z");
  private static final String QUERY_PHOTOS = "?workflows=inbound&"
      + "logistic_center=ARBA01&"
      + "date_out_from=2023-01-02T03:00:00Z&"
      + "date_out_to=2023-01-02T19:27:43Z&"
      + "date_from=2023-01-02T03:00:00Z&"
      + "date_to=2023-01-02T19:32:43Z&"
      + "steps=FINISHED&"
      + "group_by=date_out";
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

  @Test
  public void testGetPhotos() throws JSONException {
    mockSuccessfulResponsePhotos();

    final Instant dateFrom = Instant.parse("2023-01-02T03:00:00Z");
    final Instant dateTo = Instant.parse("2023-01-02T19:32:43Z");
    final Instant dateOutFrom = Instant.parse("2023-01-02T03:00:00Z");
    final Instant dateOutTo = Instant.parse("2023-01-02T19:27:43Z");

    // WHEN
    final List<Photo> result = client.getPhotos(
        new PhotoRequest(
            of(FBM_WMS_INBOUND, INBOUND_TRANSFER),
            "ARBA01",
            of("FINISHED"),
            dateFrom,
            dateTo,
            Instant.now(),
            Instant.now(),
            dateOutFrom,
            dateOutTo,
            of(DATE_OUT, WORKFLOW)
        )
    );

    final var res = result.get(0);
    final var group0 = res.getGroups().get(0);
    final var group1 = res.getGroups().get(1);

    assertEquals(res.getTakenOn(), Instant.parse("2023-01-02T04:00:00Z"));
    assertNotNull(group0.getKey().get(DATE_OUT.getName()));
    assertEquals(group0.getKey().get(DATE_OUT.getName()), "2023-01-02T09:00:00Z");

    assertNotNull(group0.getKey().get(WORKFLOW.getName()));
    assertEquals(group0.getKey().get(WORKFLOW.getName()), "inbound");

    assertEquals(group0.getTotal(), 2147);
    assertEquals(group0.getAccumulatedTotal(), 2147);

    assertNotNull(group1.getKey().get(DATE_OUT.getName()));
    assertEquals(group1.getKey().get(DATE_OUT.getName()), "2023-01-02T09:00:00Z");

    assertNotNull(group1.getKey().get(WORKFLOW.getName()));
    assertEquals(group1.getKey().get(WORKFLOW.getName()), "inbound-transfer");

    assertEquals(group1.getTotal(), 1000);
    assertEquals(group1.getAccumulatedTotal(), 1000);
  }

  @Test
  public void testGetPhotoErr() {
    // GIVEN
    final String url = String.format(URL, WAREHOUSE_ID) + QUERY;

    mockErroneousResponsePhoto(url);

    final Instant dateFrom = Instant.parse("2023-01-02T03:00:00Z");
    final Instant dateTo = Instant.parse("2023-01-02T19:32:43Z");
    final Instant dateOutFrom = Instant.parse("2023-01-02T03:00:00Z");
    final Instant dateOutTo = Instant.parse("2023-01-02T19:27:43Z");

    // WHEN
    assertThrows(ClientException.class, () ->
        client.getPhotos(
            new PhotoRequest(
                of(FBM_WMS_INBOUND, INBOUND_TRANSFER),
                "ARBA01",
                of("FINISHED"),
                dateFrom,
                dateTo,
                Instant.now(),
                Instant.now(),
                dateOutFrom,
                dateOutTo,
                of(DATE_OUT, WORKFLOW)
            )
        ));
  }

  @Test
  public void testGetLastPhoto() throws JSONException {
    mockSuccessfulResponseLastPhoto();

    final Instant dateOutFrom = Instant.parse("2023-01-02T03:00:00Z");
    final Instant dateOutTo = Instant.parse("2023-01-02T19:27:43Z");

    // WHEN
    final Photo result = client.getLastPhoto(
        new LastPhotoRequest(
            of(FBM_WMS_INBOUND, INBOUND_TRANSFER),
            "ARBA01",
            of("FINISHED"),
            null,
            null,
            Instant.now(),
            Instant.now(),
            dateOutFrom,
            dateOutTo,
            of(DATE_OUT, WORKFLOW),
            Instant.now()
        )
    );

    final var group0 = result.getGroups().get(0);
    final var group1 = result.getGroups().get(1);

    assertEquals(result.getTakenOn(), Instant.parse("2023-01-02T04:00:00Z"));
    assertNotNull(group0.getKey().get(DATE_OUT.getName()));
    assertEquals(group0.getKey().get(DATE_OUT.getName()), "2023-01-02T09:00:00Z");

    assertNotNull(group0.getKey().get(WORKFLOW.getName()));
    assertEquals(group0.getKey().get(WORKFLOW.getName()), "inbound");

    assertEquals(group0.getTotal(), 2147);
    assertEquals(group0.getAccumulatedTotal(), 2147);

    assertNotNull(group1.getKey().get(DATE_OUT.getName()));
    assertEquals(group1.getKey().get(DATE_OUT.getName()), "2023-01-02T09:00:00Z");

    assertNotNull(group1.getKey().get(WORKFLOW.getName()));
    assertEquals(group1.getKey().get(WORKFLOW.getName()), "inbound-transfer");

    assertEquals(group1.getTotal(), 1000);
    assertEquals(group1.getAccumulatedTotal(), 1000);
  }

  @Test
  public void testGetLastPhotoErr() {
    final String url = String.format(URL + "/last", WAREHOUSE_ID) + QUERY;

    // GIVEN
    mockErroneousResponsePhoto(url);

    final Instant dateFrom = Instant.parse("2023-01-02T03:00:00Z");
    final Instant dateTo = Instant.parse("2023-01-02T19:32:43Z");
    final Instant dateOutFrom = Instant.parse("2023-01-02T03:00:00Z");
    final Instant dateOutTo = Instant.parse("2023-01-02T19:27:43Z");

    // WHEN
    assertThrows(ClientException.class, () ->
        client.getPhotos(
            new PhotoRequest(
                of(FBM_WMS_INBOUND, INBOUND_TRANSFER),
                "ARBA01",
                of("FINISHED"),
                dateFrom,
                dateTo,
                Instant.now(),
                Instant.now(),
                dateOutFrom,
                dateOutTo,
                of(DATE_OUT, WORKFLOW)
            )
        ));
  }

  private void mockSuccessfulResponsePhotos() throws JSONException {
    final String url = String.format(PHOTO_URL, WAREHOUSE_ID) + QUERY_PHOTOS;

    MockResponse.builder()
        .withMethod(GET)
        .withURL(BASE_URL_ROUTE + url)
        .withStatusCode(OK.value())
        .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
        .withResponseBody(buildResponsePhotos().toString())
        .build();
  }

  private void mockSuccessfulResponseLastPhoto() throws JSONException {
    final String url = String.format(PHOTO_URL + "/last", WAREHOUSE_ID) + QUERY_PHOTOS;

    MockResponse.builder()
        .withMethod(GET)
        .withURL(BASE_URL_ROUTE + url)
        .withStatusCode(OK.value())
        .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
        .withResponseBody(buildResponseLastPhoto().toString())
        .build();
  }

  private JSONObject buildResponseLastPhoto() throws JSONException {
    return new JSONObject()
        .put("taken_on", "2023-01-02T04:00:00Z")
        .put("groups", new JSONArray()
            .put(
                new JSONObject()
                    .put("key",
                        new JSONObject(
                            Map.of("date_out", "2023-01-02T09:00:00Z",
                                "workflow", "inbound")
                        )
                    )
                    .put("total", 2147)
                    .put("accumulated_total", 2147)
            )
            .put(
                new JSONObject()
                    .put("key",
                        new JSONObject(
                            Map.of("date_out", "2023-01-02T09:00:00Z",
                                "workflow", "inbound-transfer")
                        )
                    )
                    .put("total", 1000)
                    .put("accumulated_total", 1000)
            )
        );
  }

  private JSONArray buildResponsePhotos() throws JSONException {
    return new JSONArray()
        .put(
            new JSONObject()
                .put("taken_on", "2023-01-02T04:00:00Z")
                .put("groups", new JSONArray()
                    .put(
                        new JSONObject()
                            .put("key",
                                new JSONObject(
                                    Map.of("date_out", "2023-01-02T09:00:00Z",
                                        "workflow", "inbound")
                                )
                            )
                            .put("total", 2147)
                            .put("accumulated_total", 2147)
                    )
                    .put(
                        new JSONObject()
                            .put("key",
                                new JSONObject(
                                    Map.of("date_out", "2023-01-02T09:00:00Z",
                                        "workflow", "inbound-transfer")
                                )
                            )
                            .put("total", 1000)
                            .put("accumulated_total", 1000)
                    )
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
    final String url = String.format(URL, WAREHOUSE_ID) + QUERY_PHOTOS;

    MockResponse.builder()
        .withMethod(GET)
        .withURL(BASE_URL_ROUTE + url)
        .withStatusCode(INTERNAL_SERVER_ERROR.value())
        .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
        .build();
  }


  private void mockErroneousResponsePhoto(final String url) {
    MockResponse.builder()
        .withMethod(GET)
        .withURL(BASE_URL_ROUTE + url)
        .withStatusCode(INTERNAL_SERVER_ERROR.value())
        .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
        .build();
  }
}

