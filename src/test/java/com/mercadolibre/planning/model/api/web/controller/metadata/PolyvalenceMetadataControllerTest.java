package com.mercadolibre.planning.model.api.web.controller.metadata;

import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.api.domain.entity.ProductivityPolyvalenceCardinality;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetPolyvalenceForecastMetadata;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.PolyvalenceMetadata;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = PolyvalenceMetadataController.class)
class PolyvalenceMetadataControllerTest {

  private static final String URL = "/planning/model/workflows/%s/metadata/polyvalences";

  private static final Instant DATE_TIME = Instant.parse("2022-06-17T11:00:00Z");

  private static final String DATE_TIME_PARAM = "date_time";

  private static final String WAREHOUSE_ID = "ARTW01";

  private static final String WAREHOUSE_ID_PARAM = "warehouse_id";


  @Autowired
  private MockMvc mvc;

  @MockBean
  private GetPolyvalenceForecastMetadata getPolyvalenceForecastMetadata;

  private static Stream<Arguments> mockArguments() {
    return Stream.of(
        Arguments.of(
            Workflow.FBM_WMS_INBOUND,
            mockPolyvalenceMetadata(Workflow.FBM_WMS_INBOUND),
            "get_inbound_polyvalences_response.json"
        ),
        Arguments.of(
            Workflow.FBM_WMS_OUTBOUND,
            mockPolyvalenceMetadata(Workflow.FBM_WMS_OUTBOUND),
            "get_outbound_polyvalences_response.json"
        )
    );
  }

  private static PolyvalenceMetadata mockPolyvalenceMetadata(final Workflow workflow) {

    return Workflow.FBM_WMS_INBOUND.equals(workflow) ?
        new PolyvalenceMetadata(
            Map.of(
                ProductivityPolyvalenceCardinality.RECEIVING_POLYVALENCE, 80.0F,
                ProductivityPolyvalenceCardinality.PUT_AWAY_POLYVALENCE, 85.0F,
                ProductivityPolyvalenceCardinality.CHECK_IN_POLYVALENCE, 75.0F
            )
        ) :
        new PolyvalenceMetadata(
            Map.of(
                ProductivityPolyvalenceCardinality.BATCH_SORTER_POLYVALENCE, 80.0F,
                ProductivityPolyvalenceCardinality.PACKING_POLYVALENCE, 85.0F,
                ProductivityPolyvalenceCardinality.PACKING_WALL_POLYVALENCE, 75.0F,
                ProductivityPolyvalenceCardinality.PICKING_POLYVALENCE, 75.0F,
                ProductivityPolyvalenceCardinality.WALL_IN_POLYVALENCE, 75.0F
            )
        );
  }

  @ParameterizedTest
  @MethodSource("mockArguments")
  @SneakyThrows
  public void testGetPolyvalenceMetadata(
      final Workflow workflow,
      final PolyvalenceMetadata polyvalenceMetadata,
      final String expectedResponse
  ) {

    when(getPolyvalenceForecastMetadata.getPolyvalencePercentage(anyString(), any(), any()))
        .thenReturn(polyvalenceMetadata);

    //WHEN
    final ResultActions result = mvc.perform(
        get(String.format(URL, workflow))
            .param(DATE_TIME_PARAM, String.valueOf(DATE_TIME))
            .param(WAREHOUSE_ID_PARAM, WAREHOUSE_ID)
    );

    // THEN
    result.andExpect(status().isOk()).andExpect(content()
        .json(getResourceAsString(expectedResponse)));
  }

  @Test
  @SneakyThrows
  public void testGetPolyvalenceMetadataException() {
    //WHEN
    final ResultActions result = mvc.perform(
        get(URL)
    );
    //THEN
    result.andExpect(status().isInternalServerError());
  }

}