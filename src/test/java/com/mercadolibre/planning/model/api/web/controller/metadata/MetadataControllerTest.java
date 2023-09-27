package com.mercadolibre.planning.model.api.web.controller.metadata;

import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastMetadata;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.Metadata;
import java.time.Instant;
import java.util.List;
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

@WebMvcTest(controllers = MetadataController.class)
public class MetadataControllerTest {
  private static final String URL = "/planning/model/workflows/%s/metadata";
  private static final Instant DATE_TIME = Instant.parse("2023-03-17T07:00:00Z");
  private static final String DATE_TIME_PARAM = "date_time";
  private static final String WAREHOUSE_ID = "ARTW01";
  private static final String WAREHOUSE_ID_PARAM = "warehouse_id";

  @Autowired
  private MockMvc mvc;

  @MockBean
  private GetForecastMetadata forecastMetadata;

  private static Stream<Arguments> mockArguments() {
    return Stream.of(
        Arguments.of(
            Workflow.FBM_WMS_OUTBOUND,
            mockMetadata(),
            "get_forecast_metadata_response.json"
        )
    );
  }

  private static List<Metadata> mockMetadata() {
    return List.of(
        new Metadata("week", "11-2023"),
        new Metadata("warehouse_id", "ARTW01"),
        new Metadata("version", "2.0"),
        new Metadata("units_per_order_ratio", "3.96"),
        new Metadata("outbound_wall_in_productivity", "100"),
        new Metadata("outbound_picking_productivity", "80"),
        new Metadata("outbound_packing_wall_productivity", "90"),
        new Metadata("outbound_packing_productivity", "100"),
        new Metadata("outbound_batch_sorter_productivity", "100"),
        new Metadata("multi_order_distribution", "26"),
        new Metadata("multi_batch_distribution", "32"),
        new Metadata("mono_order_distribution", "42")
    );
  }

  @ParameterizedTest
  @MethodSource("mockArguments")
  @SneakyThrows
  public void testMetadata(
      final Workflow workflow,
      final List<Metadata> metadata,
      final String expectedResponse
  ) {

    when(forecastMetadata.getMetadata(anyString(), any(), any()))
        .thenReturn(metadata);

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
  public void testMetadataException() {
    //WHEN
    final ResultActions result = mvc.perform(
        get(URL)
    );
    //THEN
    result.andExpect(status().isBadRequest());
  }
}
