package com.mercadolibre.planning.model.api.web.controller;

import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockGetSuggestedWavesInput;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.api.domain.entity.WaveCardinality;
import com.mercadolibre.planning.model.api.domain.usecase.suggestedwave.get.GetSuggestedWavesInput;
import com.mercadolibre.planning.model.api.domain.usecase.suggestedwave.get.GetSuggestedWavesUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.suggestedwave.get.SuggestedWavesOutput;
import com.mercadolibre.planning.model.api.web.controller.suggestedwave.SuggestedWavesController;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = SuggestedWavesController.class)
public class SuggestedWavesControllerTest {

  private static final String URL =
      "/planning/model/workflows/{workflow}/projections/suggested_waves";

  @Autowired
  private MockMvc mvc;

  @MockBean
  private GetSuggestedWavesUseCase getSuggestedWavesUseCase;

  @DisplayName("Get Suggested Wave")
  @Test
  public void testGetSuggestedWave() throws Exception {
    // GIVEN
    final GetSuggestedWavesInput input = mockGetSuggestedWavesInput();

    when(getSuggestedWavesUseCase.execute(input))
        .thenReturn(mockSuggestedWavesOutput());

    // WHEN
    final ResultActions result = mvc.perform(
        get(URL, "fbm-wms-outbound")
            .contentType(APPLICATION_JSON)
            .param("warehouse_id", "ARBA01")
            .param("date_from", A_DATE_UTC.toString())
            .param("date_to", A_DATE_UTC.plusHours(2).toString())
            .param("backlog", String.valueOf(230))
            .param("apply_deviation", "true")
    );

    // THEN
    result.andExpect(status().isOk());
  }

  private List<SuggestedWavesOutput> mockSuggestedWavesOutput() {
    return List.of(
        new SuggestedWavesOutput(
            WaveCardinality.MONO_ORDER_DISTRIBUTION,
            56L
        ),
        new SuggestedWavesOutput(
            WaveCardinality.MULTI_ORDER_DISTRIBUTION,
            79L
        ),
        new SuggestedWavesOutput(
            WaveCardinality.MULTI_BATCH_DISTRIBUTION,
            36L
        )
    );
  }


}
