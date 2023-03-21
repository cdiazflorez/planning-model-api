package com.mercadolibre.planning.model.api.domain.usecase.forecast.get;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastMetadata.LastForecastRepository;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastMetadata.MetadataRepository;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GetForecastMetadataTest {
  private static final String WAREHOUSE_ID = "ARTW01";
  private static final ZonedDateTime DATE_TIME = ZonedDateTime.now();

  @InjectMocks
  private GetForecastMetadata forecastMetadata;
  @Mock
  private LastForecastRepository lastForecastRepository;
  @Mock
  private MetadataRepository metadataRepository;

  private static Stream<Arguments> mockArguments() {
    return Stream.of(
        Arguments.of(
            Workflow.FBM_WMS_OUTBOUND,
            4L,
            mockMetadata()
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
  public void testGetForecastMetadata(
      final Workflow workflow,
      final Long forecastId,
      final List<Metadata> metadata
  ) {

    when(lastForecastRepository.getForecastByWorkflow(anyString(), any(), anyString()))
        .thenReturn(forecastId);

    when(metadataRepository.getMetadataByTag(anyLong()))
        .thenReturn(metadata);

    final List<Metadata> result =
        forecastMetadata.getMetadata(WAREHOUSE_ID, workflow, DATE_TIME);


    Assertions.assertNotNull(result);
    Assertions.assertEquals(metadata.size(), result.size());
  }
}
