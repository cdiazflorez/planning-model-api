package com.mercadolibre.planning.model.api.domain.usecase.forecast.get;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.domain.entity.ProductivityPolyvalenceCardinality;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetPolyvalenceForecastMetadata.LastForecastRepository;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetPolyvalenceForecastMetadata.PolyvalenceMetadataRepository;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
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
class GetPolyvalenceForecastMetadataTest {

  private static final String WAREHOUSE_ID = "ARTW01";
  private static final ZonedDateTime DATE_TIME =
      ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

  @InjectMocks
  private GetPolyvalenceForecastMetadata getPolyvalenceForecastMetadata;

  @Mock
  private LastForecastRepository lastForecastRepository;

  @Mock
  private PolyvalenceMetadataRepository polyvalenceMetadataRepository;

  private static Stream<Arguments> mockArguments() {
    return Stream.of(
        Arguments.of(
            Workflow.FBM_WMS_INBOUND,
            2L,
            mockPolyvalenceMetadata(Workflow.FBM_WMS_INBOUND)
        ),
        Arguments.of(
            Workflow.FBM_WMS_OUTBOUND,
            4L,
            mockPolyvalenceMetadata(Workflow.FBM_WMS_OUTBOUND)
        )
    );
  }

  private static PolyvalenceMetadata mockPolyvalenceMetadata(final Workflow workflow) {

    return Workflow.FBM_WMS_INBOUND.equals(workflow)
        ? new PolyvalenceMetadata(Map.of(
        ProductivityPolyvalenceCardinality.RECEIVING_POLYVALENCE, 80.0F,
        ProductivityPolyvalenceCardinality.PUT_AWAY_POLYVALENCE, 85.0F,
        ProductivityPolyvalenceCardinality.CHECK_IN_POLYVALENCE, 75.0F
    ))
        : new PolyvalenceMetadata(Map.of(
        ProductivityPolyvalenceCardinality.BATCH_SORTER_POLYVALENCE, 80.0F,
        ProductivityPolyvalenceCardinality.PACKING_POLYVALENCE, 85.0F,
        ProductivityPolyvalenceCardinality.PACKING_WALL_POLYVALENCE, 75.0F,
        ProductivityPolyvalenceCardinality.PICKING_POLYVALENCE, 75.0F,
        ProductivityPolyvalenceCardinality.WALL_IN_POLYVALENCE, 75.0F
    ));
  }

  @ParameterizedTest
  @MethodSource("mockArguments")
  public void testGetPolyvalencePercentage(
      final Workflow workflow,
      final Long forecastId,
      final PolyvalenceMetadata polyvalenceMetadata
  ) {

    when(lastForecastRepository.getForecastByWorkflow(anyString(), any(), anyString()))
        .thenReturn(forecastId);

    when(polyvalenceMetadataRepository.getPolyvalencePercentageByWorkflow(anyLong(), any()))
        .thenReturn(polyvalenceMetadata);

    final PolyvalenceMetadata result =
        getPolyvalenceForecastMetadata.getPolyvalencePercentage(WAREHOUSE_ID, workflow, DATE_TIME);


    Assertions.assertNotNull(result);
    Assertions.assertEquals(polyvalenceMetadata.getPolyvalences().size(), result.getPolyvalences().size());

  }
}
