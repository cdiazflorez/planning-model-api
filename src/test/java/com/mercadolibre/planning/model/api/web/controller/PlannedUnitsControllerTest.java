package com.mercadolibre.planning.model.api.web.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.api.web.controller.forecast.PlannedUnitsController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = PlannedUnitsController.class)
public class PlannedUnitsControllerTest {

  private static final String URL = "/logistic_center/{logistic_center}/plan/units";
  private static final String WH = "ARBA01";
  private static final String DATE_IN_FROM = "2022-09-10T10:00:00Z";
  private static final String DATE_IN_TO = "2022-09-10T14:00:00Z";
  private static final String DATE_OUT_FROM = "2022-09-10T13:00:00Z";
  private static final String DATE_OUT_TO = "2022-09-10T14:00:00Z";
  private static final String WORKFLOW = "fbm-wms-outbound";
  private static final String PROCESS_PATHS = "tot_mono,non_tot_mono";
  private static final String GROUP_BY = "process_path,date_in,date_out";

  private static final String DATE_IN_FROM_PARAM = "date_in_from";
  private static final String DATE_IN_TO_PARAM = "date_in_to";
  private static final String DATE_OUT_FROM_PARAM = "date_out_from";
  private static final String DATE_OUT_TO_PARAM = "date_out_to";
  private static final String WORKFLOW_PARAM = "workflow";
  private static final String PROCESS_PATHS_PARAM = "process_paths";
  private static final String GROUP_BY_PARAM = "group_by";
  private static final String VIEW_DATE_PARAM = "view_date";

  @Autowired
  private MockMvc mvc;

  @Test
  public void testGetForecast() throws Exception {

    // WHEN
    final ResultActions result = mvc.perform(
        get(URL, WH)
            .contentType(APPLICATION_JSON)
            .param(WORKFLOW_PARAM, WORKFLOW)
            .param(PROCESS_PATHS_PARAM, PROCESS_PATHS)
            .param(DATE_IN_FROM_PARAM, DATE_IN_FROM)
            .param(DATE_IN_TO_PARAM, DATE_IN_TO)
            .param(DATE_OUT_FROM_PARAM, DATE_OUT_FROM)
            .param(DATE_OUT_TO_PARAM, DATE_OUT_TO)
            .param(VIEW_DATE_PARAM, DATE_IN_FROM)
            .param(GROUP_BY_PARAM, GROUP_BY)
    );

    // WHEN
    final ResultActions resultTwo = mvc.perform(
        get(URL, WH)
            .contentType(APPLICATION_JSON)
            .param(WORKFLOW_PARAM, WORKFLOW)
            .param(PROCESS_PATHS_PARAM, PROCESS_PATHS)
            .param(DATE_OUT_FROM_PARAM, DATE_OUT_FROM)
            .param(DATE_OUT_TO_PARAM, DATE_OUT_TO)
            .param(VIEW_DATE_PARAM, DATE_IN_FROM)
            .param(GROUP_BY_PARAM, GROUP_BY)
    );

    result.andExpect(status().isOk());
    resultTwo.andExpect(status().isOk());

  }

  @Test
  public void testGetForecastError() throws Exception {

    // WHEN
    final ResultActions result = mvc.perform(
        get(URL, WH)
            .contentType(APPLICATION_JSON)
            .param(WORKFLOW_PARAM, WORKFLOW)
            .param(PROCESS_PATHS_PARAM, PROCESS_PATHS)
            .param(DATE_IN_FROM_PARAM, DATE_IN_FROM)
            .param(DATE_OUT_TO_PARAM, DATE_OUT_TO)
            .param(VIEW_DATE_PARAM, DATE_IN_FROM)
            .param(GROUP_BY_PARAM, GROUP_BY)
    );

    // WHEN
    final ResultActions resultTwo = mvc.perform(
        get(URL, WH)
            .contentType(APPLICATION_JSON)
            .param(WORKFLOW_PARAM, WORKFLOW)
            .param(PROCESS_PATHS_PARAM, PROCESS_PATHS)
            .param(VIEW_DATE_PARAM, DATE_IN_FROM)
            .param(GROUP_BY_PARAM, GROUP_BY)
    );

    // WHEN
    final ResultActions resultThree = mvc.perform(
        get(URL, WH)
            .contentType(APPLICATION_JSON)
            .param(WORKFLOW_PARAM, WORKFLOW)
            .param(PROCESS_PATHS_PARAM, PROCESS_PATHS)
            .param(DATE_IN_TO_PARAM, DATE_IN_TO)
            .param(DATE_OUT_FROM_PARAM, DATE_OUT_FROM)
            .param(VIEW_DATE_PARAM, DATE_IN_FROM)
            .param(GROUP_BY_PARAM, GROUP_BY)
    );

    result.andExpect(status().is4xxClientError());
    resultTwo.andExpect(status().is4xxClientError());
    resultThree.andExpect(status().is4xxClientError());

  }
}
