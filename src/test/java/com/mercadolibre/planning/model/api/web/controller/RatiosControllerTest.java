package com.mercadolibre.planning.model.api.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.api.domain.usecase.ratios.GetPackingWallRatiosService;
import com.mercadolibre.planning.model.api.web.controller.ratios.RatiosController;
import com.mercadolibre.planning.model.api.web.controller.ratios.response.PackingRatio;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = RatiosController.class)
class RatiosControllerTest {

  private static final String DATE_FROM = "2023-01-25T18:30:00Z";

  private static final String DATE_TO = "2023-02-03T21:30:00Z";

  private static final String ARTW01 = "ARTW01";

  private static final String URL = "/logistic_center/{logisticCenterId}/ratios";

  @Autowired
  private MockMvc mvc;

  @MockBean
  private GetPackingWallRatiosService getPackingWallRatiosUseCase;

  @Test
  void testRequest() throws Exception {
    when(getPackingWallRatiosUseCase.execute(anyString(), any(), any()))
        .thenReturn(Map.of(Instant.parse(DATE_FROM), new PackingRatio(0.5, 0.5)));

    final ResultActions result = mvc.perform(
        get(URL + "/packing_wall", ARTW01)
            .contentType(APPLICATION_JSON)
            .param("date_from", DATE_FROM)
            .param("date_to", DATE_TO)
    );
    result.andExpect(status().isOk());
    result.andExpect(content().json("{\"2023-01-25T18:30:00Z\":{\"packing_tote_ratio\":0.5,\"packing_wall_ratio\":0.5}}"));
  }

  @Test
  void testWithDateFromGreaterThanDateTo() throws Exception {
    final ResultActions result = mvc.perform(
        get(URL + "/packing_wall", ARTW01)
            .contentType(APPLICATION_JSON)
            .param("logistic_center_id", ARTW01)
            .param("date_from", DATE_TO)
            .param("date_to", DATE_FROM)
    );
    result.andExpect(status().isBadRequest());
  }
}
