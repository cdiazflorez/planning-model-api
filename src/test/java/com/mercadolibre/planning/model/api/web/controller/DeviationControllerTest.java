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
import java.util.Optional;
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

  private static final String SAVE = "/save";

  private static final String UNITS = "units";

  private static final String URL_TYPE_SAVE = "/planning/model/workflows/{workflow}/deviations/save/{type}";

  @Autowired
  private MockMvc mvc;

  @MockBean
  private SaveDeviationUseCase saveDeviationUseCase;

  @MockBean
  private DisableForecastDeviationUseCase disableDeviationUseCase;

  @MockBean
  private GetForecastDeviationUseCase getForecastDeviationUseCase;

  @DisplayName("Save forecast deviation ")
  @Test
  void saveDeviationOk() throws Exception {
    // GIVEN
    final SaveDeviationInput input = mockSaveForecastDeviationInput();

    when(saveDeviationUseCase.execute(input))
        .thenReturn(new DeviationResponse(200));

    // WHEN
    final ResultActions result = mvc.perform(
        post(URL + SAVE, "fbm-wms-outbound")
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString("post_forecast_deviation.json"))
    );

    // THEN
    result.andExpect(status().isOk());
  }

  @DisplayName("Disable forecast deviation ")
  @Test
  void disableDeviationOutboundOk() throws Exception {
    // GIVEN
    final DisableForecastDeviationInput input = mockDisableForecastDeviationInput(FBM_WMS_OUTBOUND, DeviationType.UNITS);

    when(disableDeviationUseCase.execute(input))
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
    result.andExpect(status().isOk());
  }

  @DisplayName("Disable deviation ")
  @Test
  void disableDeviationOk() throws Exception {
    // GIVEN
    final DisableForecastDeviationInput input = mockDisableForecastDeviationInput(FBM_WMS_INBOUND, DeviationType.UNITS);


    when(disableDeviationUseCase.execute(input))
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
            Optional.of(
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

  @DisplayName("Save outbound deviation type unit")
  @Test
  void saveDeviationOutboundUnitOk() throws Exception {
    // GIVEN
    final SaveDeviationInput input = mockSaveForecastDeviationInput();

    when(saveDeviationUseCase.execute(input))
        .thenReturn(new DeviationResponse(200));

    // WHEN
    final ResultActions result = mvc.perform(
        post(URL_TYPE_SAVE, "fbm-wms-outbound", UNITS)
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString("post_forecast_deviation.json"))
    );

    // THEN
    result.andExpect(status().isOk());
  }

  @DisplayName("Save inbound deviation type unit")
  @Test
  void saveDeviationInboundUnitOk() throws Exception {
    // GIVEN
    final SaveDeviationInput input = SaveDeviationInput
        .builder()
        .workflow(FBM_WMS_INBOUND)
        .dateFrom(DATE_IN)
        .dateTo(DATE_OUT)
        .value(600)
        .userId(1234L)
        .warehouseId(WAREHOUSE_ID)
        .build();

    when(saveDeviationUseCase.execute(input))
        .thenReturn(new DeviationResponse(200));

    // WHEN
    final ResultActions result = mvc.perform(
        post(URL_TYPE_SAVE, "inbound", UNITS)
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString("post_inbound_deviation_save_unit.json"))
    );

    // THEN
    result.andExpect(status().isOk());
  }


  @DisplayName("Save inbound deviation type unit path error deviation type")
  @Test
  void saveDeviationInboundUnitPathError() throws Exception {

    // WHEN
    final ResultActions result = mvc.perform(
        post(URL_TYPE_SAVE, "inbound-transfer", "error-type")
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString("post_inbound_deviation_save_unit.json"))
    );

    // THEN
    result.andExpect(status().isInternalServerError());
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
            Optional.of(
                GetForecastDeviationResponse.builder()
                    .workflow(INBOUND)
                    .dateFrom(DATE_IN)
                    .dateTo(DATE_OUT)
                    .value(250)
                    .metricUnit(MetricUnit.UNITS)
                    .build()
            )
        );

    when(getForecastDeviationUseCase.execute(inboundTransferRequest))
        .thenReturn(
            Optional.of(
                GetForecastDeviationResponse.builder()
                    .workflow(INBOUND_TRANSFER)
                    .dateFrom(DATE_IN)
                    .dateTo(DATE_OUT)
                    .value(300)
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

            jsonPath("$[0].workflow", is("inbound")),
            jsonPath("$[0].date_from", is("2020-08-19T18:00:00Z")),
            jsonPath("$[0].date_to", is("2020-08-20T15:30:00Z")),
            jsonPath("$[0].value", is(2.5)),

            jsonPath("$[1].workflow", is("inbound-transfer")),
            jsonPath("$[1].date_from", is("2020-08-19T18:00:00Z")),
            jsonPath("$[1].date_to", is("2020-08-20T15:30:00Z")),
            jsonPath("$[1].value", is(3.0))
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
            Optional.of(
                GetForecastDeviationResponse.builder()
                    .workflow(INBOUND)
                    .dateFrom(DATE_IN)
                    .dateTo(DATE_OUT)
                    .value(250)
                    .metricUnit(MetricUnit.UNITS)
                    .build()
            )
        );

    when(getForecastDeviationUseCase.execute(inboundTransferRequest))
        .thenReturn(Optional.empty());

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
            jsonPath("$[0].workflow", is("inbound")),
            jsonPath("$[0].date_from", is("2020-08-19T18:00:00Z")),
            jsonPath("$[0].date_to", is("2020-08-20T15:30:00Z")),
            jsonPath("$[0].value", is(2.5))
        );
  }

}
