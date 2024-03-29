package com.mercadolibre.planning.model.api.web.controller;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.PERCENTAGE;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.INBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.INBOUND_TRANSFER;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE_IN;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE_OUT;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockDisableForecastDeviationInput;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockSaveForecastDeviationInput;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.api.domain.entity.DeviationType;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.Path;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.disable.DisableForecastDeviationInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.disable.DisableForecastDeviationUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.get.GetForecastDeviationInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.get.GetForecastDeviationUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.save.SaveDeviationInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.save.SaveDeviationUseCase;
import com.mercadolibre.planning.model.api.web.controller.deviation.DeviationController;
import com.mercadolibre.planning.model.api.web.controller.deviation.response.DeviationResponse;
import com.mercadolibre.planning.model.api.web.controller.deviation.response.GetForecastDeviationResponse;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.util.LinkedMultiValueMap;

@WebMvcTest(controllers = DeviationController.class)
class DeviationControllerTest {

  private static final String URL = "/planning/model/workflows/{workflow}/deviations";

  private static final String WAREHOUSE_LABEL = "warehouse_id";

  private static final String LOGISTIC_CENTER_LABEL = "logistic_center_id";

  private static final String VIEW_DATE_LABEL = "view_date";

  private static final String DISABLE_ALL = "/disable/all";

  private static final String SEARCH = "/search";

  private static final String UNITS = "units";

  private static final String URL_TYPE_SAVE = "/planning/model/workflows/{workflow}/deviations/save/{type}";
  private static final String INBOUND_WORKFLOW = "inbound";

  @Autowired
  private MockMvc mvc;

  @MockBean
  private SaveDeviationUseCase saveDeviationUseCase;

  @MockBean
  private DisableForecastDeviationUseCase disableDeviationUseCase;

  @MockBean
  private GetForecastDeviationUseCase getForecastDeviationUseCase;

  private static Stream<Arguments> saveDeviationCases() throws JSONException {
    final ZonedDateTime currentDate = Instant.now().truncatedTo(ChronoUnit.HOURS).atZone(ZoneOffset.UTC);
    return Stream.of(
        Arguments.of(
            FBM_WMS_OUTBOUND.toJson(),
            buildDeviationRequest(
                FBM_WMS_OUTBOUND.toJson(),
                currentDate.minus(1, ChronoUnit.HOURS),
                currentDate.minus(2, ChronoUnit.HOURS)
            ),
            status().isBadRequest()
        ),
        Arguments.of(
            FBM_WMS_OUTBOUND.toJson(),
            buildDeviationRequest(
                FBM_WMS_OUTBOUND.toJson(),
                currentDate.minus(1, ChronoUnit.HOURS),
                currentDate.plus(2, ChronoUnit.HOURS)
            ),
            status().isBadRequest()
        ),
        Arguments.of(
            FBM_WMS_OUTBOUND.toJson(),
            buildDeviationRequest(
                FBM_WMS_OUTBOUND.toJson(),
                currentDate.minus(3, ChronoUnit.HOURS),
                currentDate.minus(2, ChronoUnit.HOURS)
            ),
            status().isBadRequest()
        ),
        Arguments.of(
            FBM_WMS_OUTBOUND.toJson(),
            new JSONObject()
                .put("logistic_center_id", WAREHOUSE_ID)
                .put("deviations", new JSONArray()),
            status().isBadRequest()
        ),
        Arguments.of(
            FBM_WMS_OUTBOUND.toJson(),
            buildDeviationRequest(
                FBM_WMS_OUTBOUND.toJson(),
                currentDate.plus(1, ChronoUnit.HOURS),
                currentDate.plus(2, ChronoUnit.HOURS)
            ),
            status().isCreated()
        )
    );
  }

  private static Stream<Arguments> listDeviationsProvided() {
    return Stream.of(
        Arguments.of(
            List.of(
                GetForecastDeviationResponse.builder()
                    .workflow(FBM_WMS_OUTBOUND)
                    .dateFrom(DATE_IN.minus(3, ChronoUnit.HOURS))
                    .dateTo(DATE_IN.minus(2, ChronoUnit.HOURS))
                    .value(0.3)
                    .metricUnit(PERCENTAGE)
                    .type(DeviationType.UNITS)
                    .build(),
                GetForecastDeviationResponse.builder()
                    .workflow(FBM_WMS_OUTBOUND)
                    .dateFrom(DATE_IN.minus(8, ChronoUnit.HOURS))
                    .dateTo(DATE_IN.minus(5, ChronoUnit.HOURS))
                    .value(0.7)
                    .metricUnit(PERCENTAGE)
                    .type(DeviationType.UNITS)
                    .build(),
                GetForecastDeviationResponse.builder()
                    .workflow(FBM_WMS_OUTBOUND)
                    .dateFrom(DATE_IN)
                    .dateTo(DATE_IN.plus(1, ChronoUnit.HOURS))
                    .value(0.4)
                    .metricUnit(PERCENTAGE)
                    .type(DeviationType.UNITS)
                    .build()

            ),
            "get_deviations_response.json"
        ),
        Arguments.of(
            emptyList(),
            "get_empty_deviations_response.json"
        )
    );
  }

  private static JSONObject buildDeviationRequest(final String workflow,
                                                  final ZonedDateTime dateIn,
                                                  final ZonedDateTime dateOut) throws JSONException {

    final JSONObject deviationItemRequest = new JSONObject();
    deviationItemRequest.put("workflow", workflow);
    deviationItemRequest.put("type", "units");
    deviationItemRequest.put("date_from", dateIn);
    deviationItemRequest.put("date_to", dateOut);
    deviationItemRequest.put("value", 0.1);
    deviationItemRequest.put("user_id", 1234);

    return new JSONObject()
        .put("logistic_center_id", WAREHOUSE_ID)
        .put("deviations", new JSONArray().put(deviationItemRequest));
  }

  @DisplayName("Disable all ok")
  @Test
  void disableAllOk() throws Exception {
    // GIVEN
    final ZonedDateTime currentDate = Instant.now().truncatedTo(ChronoUnit.HOURS).atZone(ZoneOffset.UTC);
    final List<DisableForecastDeviationInput> inputs = List.of(
        new DisableForecastDeviationInput(
            WAREHOUSE_ID,
            INBOUND,
            DeviationType.UNITS,
            List.of(Path.SPD, Path.COLLECT)),
        new DisableForecastDeviationInput(
            WAREHOUSE_ID,
            INBOUND_TRANSFER,
            DeviationType.UNITS,
            List.of())
    );

    when(disableDeviationUseCase.execute(inputs, currentDate))
        .thenReturn(200);

    // WHEN
    final ResultActions result = mvc.perform(
        post(URL + DISABLE_ALL, INBOUND_WORKFLOW)
            .contentType(APPLICATION_JSON)
            .param(LOGISTIC_CENTER_LABEL, WAREHOUSE_ID)
            .content(getResourceAsString("post_disable_all_deviation.json"))
    );

    // THEN
    result.andExpect(status().isOk());
  }

  @DisplayName("Get forecast deviation ")
  @Test
  void testGetDeviationOk() throws Exception {
    // GIVEN
    final LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap();
    params.add(WAREHOUSE_LABEL, WAREHOUSE_ID);
    params.add("date", "2020-08-19T18:00:00Z");

    when(getForecastDeviationUseCase.execute(any(GetForecastDeviationInput.class)))
        .thenReturn(
            List.of(
                GetForecastDeviationResponse.builder()
                    .dateFrom(DATE_IN)
                    .dateTo(DATE_OUT)
                    .value(2.5)
                    .metricUnit(PERCENTAGE)
                    .build()
            )
        );

    // WHEN
    final ResultActions result = mvc.perform(
        get(URL, FBM_WMS_OUTBOUND)
            .params(params)
            .contentType(APPLICATION_JSON)
    );

    // THEN
    result.andExpect(status().isOk())
        .andExpect(content().json(getResourceAsString("get_deviation_response.json")));
  }

  @Test
  @DisplayName("On get active deviations then return one deviation per workflow")
  void testGetActiveDeviations() throws Exception {
    // GIVEN
    final LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap();
    params.add(WAREHOUSE_LABEL, WAREHOUSE_ID);
    params.add("date", "2020-08-19T18:00:00Z");
    params.add("workflows", INBOUND.getName());
    params.add("workflows", INBOUND_TRANSFER.getName());

    final var date = ZonedDateTime.parse("2020-08-19T18:00:00Z");
    final var inboundRequest = new GetForecastDeviationInput(WAREHOUSE_ID, INBOUND, date);
    final var inboundTransferRequest = new GetForecastDeviationInput(WAREHOUSE_ID, INBOUND_TRANSFER, date);

    when(getForecastDeviationUseCase.execute(inboundRequest))
        .thenReturn(
            List.of(
                GetForecastDeviationResponse.builder()
                    .workflow(INBOUND)
                    .dateFrom(DATE_IN)
                    .dateTo(DATE_OUT)
                    .value(0.25)
                    .metricUnit(MetricUnit.UNITS)
                    .build()
            )
        );

    when(getForecastDeviationUseCase.execute(inboundTransferRequest))
        .thenReturn(
            List.of(
                GetForecastDeviationResponse.builder()
                    .workflow(INBOUND_TRANSFER)
                    .dateFrom(DATE_IN)
                    .dateTo(DATE_OUT)
                    .value(0.3)
                    .metricUnit(MetricUnit.UNITS)
                    .build()
            )
        );

    // WHEN
    final ResultActions result = mvc.perform(
        get(URL + "/active", FBM_WMS_OUTBOUND)
            .params(params)
            .contentType(APPLICATION_JSON)
    );

    // THEN
    result.andExpect(status().isOk())
        .andExpectAll(
            jsonPath("$.length()", is(2)),

            jsonPath("$[0].workflow", is(INBOUND_WORKFLOW)),
            jsonPath("$[0].date_from", is("2020-08-19T18:00:00Z")),
            jsonPath("$[0].date_to", is("2020-08-20T15:30:00Z")),
            jsonPath("$[0].value", is(0.25)),

            jsonPath("$[1].workflow", is("inbound-transfer")),
            jsonPath("$[1].date_from", is("2020-08-19T18:00:00Z")),
            jsonPath("$[1].date_to", is("2020-08-20T15:30:00Z")),
            jsonPath("$[1].value", is(0.3))
        );
  }

  @Test
  @DisplayName("On get active deviations when there is one missing deviation then return value for the other")
  void testGetActiveDeviationsWhenAtLeastOneIsMissing() throws Exception {
    // GIVEN
    final LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap();
    params.add(WAREHOUSE_LABEL, WAREHOUSE_ID);
    params.add("date", "2020-08-19T18:00:00Z");
    params.add("workflows", INBOUND.getName());
    params.add("workflows", INBOUND_TRANSFER.getName());

    final var date = ZonedDateTime.parse("2020-08-19T18:00:00Z");
    final var inboundRequest = new GetForecastDeviationInput(WAREHOUSE_ID, INBOUND, date);
    final var inboundTransferRequest = new GetForecastDeviationInput(WAREHOUSE_ID, INBOUND_TRANSFER, date);

    when(getForecastDeviationUseCase.execute(inboundRequest))
        .thenReturn(
            List.of(
                GetForecastDeviationResponse.builder()
                    .workflow(INBOUND)
                    .dateFrom(DATE_IN)
                    .dateTo(DATE_OUT)
                    .value(0.25)
                    .metricUnit(MetricUnit.UNITS)
                    .build()
            )
        );

    when(getForecastDeviationUseCase.execute(inboundTransferRequest))
        .thenReturn(emptyList());

    // WHEN
    final ResultActions result = mvc.perform(
        get(URL + "/active", FBM_WMS_OUTBOUND)
            .params(params)
            .contentType(APPLICATION_JSON)
    );

    // THEN
    result.andExpect(status().isOk())
        .andExpectAll(
            jsonPath("$.length()", is(1)),
            jsonPath("$[0].workflow", is(INBOUND_WORKFLOW)),
            jsonPath("$[0].date_from", is("2020-08-19T18:00:00Z")),
            jsonPath("$[0].date_to", is("2020-08-20T15:30:00Z")),
            jsonPath("$[0].value", is(0.25))
        );
  }

  @DisplayName("Save forecast deviation /save")
  @ParameterizedTest
  @MethodSource("saveDeviationCases")
  void saveDeviationsOk(final String workflow, final JSONObject request, final ResultMatcher status) throws Exception {

    // WHEN
    final ResultActions result = mvc.perform(
        post(URL, workflow)
            .contentType(APPLICATION_JSON)
            .content(request.toString())
    );

    // THEN
    result.andExpect(status);
  }

  @DisplayName("Get deviations list ")
  @ParameterizedTest
  @MethodSource("listDeviationsProvided")
  void testGetDeviations(final List<GetForecastDeviationResponse> deviations, final String expectedResponse) throws Exception {
    // GIVEN

    when(getForecastDeviationUseCase.execute(any(GetForecastDeviationInput.class)))
        .thenReturn(deviations);

    // WHEN
    final ResultActions result = mvc.perform(
        get(URL + SEARCH, FBM_WMS_OUTBOUND)
            .param(LOGISTIC_CENTER_LABEL, WAREHOUSE_ID)
            .param(VIEW_DATE_LABEL, "2020-08-19T18:00:00Z")
            .contentType(APPLICATION_JSON)
    );

    // THEN
    result.andExpect(status().isOk())
        .andExpect(content().json(getResourceAsString(expectedResponse)));
  }

  @DisplayName("Save outbound deviation type unit not found")
  @Test
  void saveDeviationTypeNotFound() throws Exception {
    // GIVEN
    final SaveDeviationInput input = mockSaveForecastDeviationInput();

    when(saveDeviationUseCase.execute(List.of(input)))
        .thenReturn(new DeviationResponse(200));

    // WHEN
    final ResultActions result = mvc.perform(
        post(URL_TYPE_SAVE, "fbm-wms-outbound", UNITS)
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString("post_forecast_deviation.json"))
    );

    // THEN
    result.andExpect(status().isNotFound());
  }

  @DisplayName("Disable forecast deviation /disable is not found")
  @Test
  void disableDeviationOutboundNotFound() throws Exception {
    // GIVEN
    final ZonedDateTime currentDate = Instant.now().truncatedTo(ChronoUnit.HOURS).atZone(ZoneOffset.UTC);
    final DisableForecastDeviationInput input = mockDisableForecastDeviationInput(FBM_WMS_OUTBOUND, DeviationType.UNITS);

    when(disableDeviationUseCase.execute(List.of(input), currentDate))
        .thenReturn(5);

    // WHEN
    final ResultActions result = mvc.perform(
        post(URL + "/disable", "fbm-wms-outbound")
            .contentType(APPLICATION_JSON)
            .param(WAREHOUSE_LABEL, WAREHOUSE_ID)
            .content(new JSONObject()
                .put(WAREHOUSE_LABEL, WAREHOUSE_ID)
                .toString()
            )
    );

    // THEN
    result.andExpect(status().isNotFound());
  }

  @DisplayName("Disable deviation /disable/{type} not found ")
  @Test
  void disableDeviationTypeNotFound() throws Exception {
    // GIVEN
    final ZonedDateTime currentDate = Instant.now().truncatedTo(ChronoUnit.HOURS).atZone(ZoneOffset.UTC);
    final DisableForecastDeviationInput input = mockDisableForecastDeviationInput(FBM_WMS_INBOUND, DeviationType.UNITS);


    when(disableDeviationUseCase.execute(List.of(input), currentDate))
        .thenReturn(5);

    // WHEN
    final ResultActions result = mvc.perform(
        post(URL + "/disable/{type}", "fbm-wms-outbound", "units")
            .contentType(APPLICATION_JSON)
            .content(new JSONObject()
                .put(WAREHOUSE_LABEL, WAREHOUSE_ID)
                .toString()
            )
    );

    // THEN
    result.andExpect(status().isNotFound());
  }
}
