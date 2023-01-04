package com.mercadolibre.planning.model.api.web.controller;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.PERCENTAGE;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE_IN;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE_OUT;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockDisableForecastDeviationInput;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockSaveForecastDeviationInput;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.api.domain.entity.DeviationType;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.disable.DisableForecastDeviationInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.disable.DisableForecastDeviationUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.get.GetForecastDeviationInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.get.GetForecastDeviationUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.save.SaveDeviationInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.save.SaveDeviationUseCase;
import com.mercadolibre.planning.model.api.web.controller.deviation.DeviationController;
import com.mercadolibre.planning.model.api.web.controller.deviation.response.DeviationResponse;
import com.mercadolibre.planning.model.api.web.controller.deviation.response.GetForecastDeviationResponse;
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
public class DeviationControllerTest {

  private static final String URL = "/planning/model/workflows/{workflow}/deviations";

  private static final String WAREHOUSE_LABEL = "warehouse_id";

  private static final String SAVE = "/save";

  private static final String UNITS = "units";

  private static final String URL_TYPE = "/planning/model/workflows/{workflow}/deviations/{type}";

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
  public void saveDeviationOk() throws Exception {
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
  public void disableDeviationOutboundOk() throws Exception {
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
  public void disableDeviationOk() throws Exception {
    // GIVEN
    final DisableForecastDeviationInput input = mockDisableForecastDeviationInput(FBM_WMS_INBOUND, DeviationType.UNITS);


    when(disableDeviationUseCase.execute(input))
        .thenReturn(5);

    // WHEN
    final ResultActions result = mvc.perform(
        post(URL + "/{type}/disable", "fbm-wms-outbound", "units")
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
  public void testGetDeviationOk() throws Exception {
    // GIVEN
    final LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap();
    params.add(WAREHOUSE_LABEL, WAREHOUSE_ID);
    params.add("date", "2020-08-19T18:00:00Z");

    when(getForecastDeviationUseCase.execute(any(GetForecastDeviationInput.class)))
        .thenReturn(GetForecastDeviationResponse.builder()
            .dateFrom(DATE_IN)
            .dateTo(DATE_OUT)
            .value(2.5)
            .metricUnit(PERCENTAGE)
            .build());

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
  public void saveDeviationOutboundUnitOk() throws Exception {
    // GIVEN
    final SaveDeviationInput input = mockSaveForecastDeviationInput();

    when(saveDeviationUseCase.execute(input))
        .thenReturn(new DeviationResponse(200));

    // WHEN
    final ResultActions result = mvc.perform(
        post(URL_TYPE + SAVE, "fbm-wms-outbound", UNITS)
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString("post_forecast_deviation.json"))
    );

    // THEN
    result.andExpect(status().isOk());
  }

  @DisplayName("Save inbound deviation type unit")
  @Test
  public void saveDeviationInboundUnitOk() throws Exception {
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
        post(URL_TYPE + SAVE, "inbound", UNITS)
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString("post_inbound_deviation_save_unit.json"))
    );

    // THEN
    result.andExpect(status().isOk());
  }


  @DisplayName("Save inbound deviation type unit path error deviation type")
  @Test
  public void saveDeviationInboundUnitPathError() throws Exception {

    // WHEN
    final ResultActions result = mvc.perform(
        post(URL_TYPE + SAVE, "inbound-transfer", null)
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString("post_inbound_deviation_save_unit.json"))
    );

    // THEN
    result.andExpect(status().isNotFound());
  }
}
