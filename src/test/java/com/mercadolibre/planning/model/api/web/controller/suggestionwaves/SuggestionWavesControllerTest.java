package com.mercadolibre.planning.model.api.web.controller.suggestionwaves;

import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = SuggestionWavesController.class)
class SuggestionWavesControllerTest {
  private static final String URL = "/logistic_center/{logisticCenterId}/projections";

  @Autowired
  private MockMvc mvc;

  @Test
  void testGetSuggestedWavesOk() throws Exception {
    // GIVEN

    // WHEN
    final ResultActions resultActions = mvc.perform(
        post(URL + "/waves", "ARTW01")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getResourceAsString("controller/waverless/request.json"))
    );

    // THEN
    resultActions.andExpect(status().isOk())
        .andExpect(content().json(getResourceAsString("controller/waverless/response.json")));
  }

}
