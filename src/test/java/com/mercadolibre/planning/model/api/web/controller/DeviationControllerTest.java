package com.mercadolibre.planning.model.api.web.controller;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.PERCENTAGE;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.INBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.INBOUND_TRANSFER;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE_IN;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE_OUT;
import static com.mercadolibre.planning.model.api.util.TestUtils.USER_ID;
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
import java.time.ZonedDateTime;
import java.util.List;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;

@WebMvcTest(controllers = DeviationController.class)
class DeviationControllerTest {

  private static final String URL = "/planning/model/workflows/{workflow}/deviations";

  private static final String WAREHOUSE_LABEL = "warehouse_id";

  private static final String LOGISTIC_CENTER_LABEL = "logistic_center_id";

  private static final String SAVE = "/save";

  private static final String SAVE_ALL = "/save/all";

  private static final String DISABLE_ALL = "/disable/all";

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

  @DisplayName("Disable all ok")
  @Test
  void disableAllOk() throws Exception {
    // GIVEN
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

    when(disableDeviationUseCase.execute(inputs))
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

  @DisplayName("Save all ok")
  @Test
  void saveAllOk() throws Exception {
    // GIVEN
    final List<SaveDeviationInput> inputs = List.of(
        SaveDeviationInput
            .builder()
            .warehouseId(WAREHOUSE_ID)
            .workflow(INBOUND)
            .deviationType(DeviationType.UNITS)
            .dateFrom(DATE_IN)
            .dateTo(DATE_OUT)
            .value(0.1)
            .userId(USER_ID)
            .paths(List.of(Path.SPD, Path.COLLECT))
            .build(),
        SaveDeviationInput
            .builder()
            .warehouseId(WAREHOUSE_ID)
            .workflow(INBOUND_TRANSFER)
            .deviationType(DeviationType.UNITS)
            .dateFrom(DATE_IN)
            .dateTo(DATE_OUT)
            .value(0.1)
            .userId(USER_ID)
            .paths(emptyList())
            .build()
    );

    when(saveDeviationUseCase.execute(inputs))
        .thenReturn(new DeviationResponse(200));

    // WHEN
    final ResultActions result = mvc.perform(
        post(URL + SAVE_ALL, INBOUND_WORKFLOW)
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString("post_save_all_deviation.json"))
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

  @DisplayName("Save forecast deviation /save is not found")
  @Test
  void saveDeviationNotFound() throws Exception {
    // GIVEN
    final SaveDeviationInput input = mockSaveForecastDeviationInput();

    when(saveDeviationUseCase.execute(List.of(input)))
        .thenReturn(new DeviationResponse(200));

    // WHEN
    final ResultActions result = mvc.perform(
        post(URL + SAVE, "fbm-wms-outbound")
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString("post_forecast_deviation.json"))
    );

    // THEN
    result.andExpect(status().isNotFound());
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
    final DisableForecastDeviationInput input = mockDisableForecastDeviationInput(FBM_WMS_OUTBOUND, DeviationType.UNITS);

    when(disableDeviationUseCase.execute(List.of(input)))
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
    final DisableForecastDeviationInput input = mockDisableForecastDeviationInput(FBM_WMS_INBOUND, DeviationType.UNITS);


    when(disableDeviationUseCase.execute(List.of(input)))
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
