package com.mercadolibre.planning.model.api.web.controller.projection.v2;

import static com.mercadolibre.planning.model.api.util.TestUtils.LOGISTIC_CENTER_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = BacklogProjectionController.class)
class BacklogProjectionControllerTest {

  private static final String ERROR_MESSAGE = "Validation failed for argument";

  private static final String URL_V2 = "/logistic_center/{logisticCenterId}/projections/backlog";

  @Autowired
  private MockMvc mvc;

  private static Stream<Arguments> calculationProjectionTotalParameters() {
    return Stream.of(
        Arguments.of("controller/projection/v2/request_no_date_from.json", ERROR_MESSAGE),
        Arguments.of("controller/projection/v2/request_no_date_to.json", ERROR_MESSAGE),
        Arguments.of("controller/projection/v2/request_no_backlog.json", ERROR_MESSAGE),
        Arguments.of("controller/projection/v2/request_no_planned_unit.json", ERROR_MESSAGE),
        Arguments.of("controller/projection/v2/request_no_throughput.json", ERROR_MESSAGE),
        Arguments.of("controller/projection/v2/request_date_from_after_date_to.json", "date_from is after date_to")
    );
  }

  @ParameterizedTest
  @MethodSource("calculationProjectionTotalParameters")
  public void testGetCalculationProjectionTotalErrors(final String requestResource, final String messageError)
      throws Exception {
    //WHEN
    ResultActions result = mvc.perform(
        post(URL_V2 + "/total", LOGISTIC_CENTER_ID)
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString(requestResource))
    );

    //THEN
    result.andExpect(status().isBadRequest());
    Assertions.assertTrue(result.andReturn().getResponse().getContentAsString().contains(messageError));
  }

  @Test
  public void testGetCalculationProjectionTotalOk()
      throws Exception {
    //WHEN
    final ResultActions result = mvc.perform(
        post(URL_V2 + "/total", LOGISTIC_CENTER_ID)
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString("controller/projection/v2/request_backlog_projection_total.json"))
    );

    //THEN
    result.andExpect(status().isOk())
        .andExpect(content().json(getResourceAsString("controller/projection/v2/response_backlog_projection_total.json")));
  }

  @Test
  public void testGetCalculationBacklogProjection() throws Exception {
    //WHEN
    final ResultActions result = mvc.perform(
        post(URL_V2, "ARTW01")
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString("post_backlog_projection.json"))
    );

    //THEN
    result.andExpectAll(
        status().isOk(),
        jsonPath("$..operation_hour").value("2023-04-10T10:00:00Z"),
        jsonPath("$..backlog[:1].process[:1].name").value("picking"),
        jsonPath("$..backlog[:1].process[:1].sla[:1].date_out").value("2023-04-10T14:00:00Z"),
        jsonPath("$..backlog[:1].process[:1].sla[:1].process_path[:1].name").value("tot_mono"),
        jsonPath("$..backlog[:1].process[:1].sla[:1].process_path[:1].quantity").value(50)
    );
  }

  @Test
  public void testGetCalculationBacklogProjectionFailUrl() throws Exception {

    // Perform the request and verify the response
    final ResultActions result = mvc.perform(post(URL_V2 + "/backlog1", "ARTW01")
        .contentType(APPLICATION_JSON)
        .content(getResourceAsString("post_backlog_projection.json")));

    // You can also perform additional assertions to verify the response body or headers
    result.andExpect(status().is4xxClientError());

  }

  @Test
  void testGetCalculationProjectionWhenBacklogIsNull() throws Exception {
    // Perform the request and verify the response
    final ResultActions result = mvc.perform(post(URL_V2, "ARTW01")
        .contentType(APPLICATION_JSON));

    result.andExpect(status().isBadRequest());
  }

}
