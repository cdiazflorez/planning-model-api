package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.domain.entity.ProductivityPolyvalenceCardinality;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.PolyvalenceMetadata;
import java.util.List;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PolyvalenceMetadataJpaRepositoryTest {

  @InjectMocks
  private PolyvalenceMetadataJpaRepository polyvalenceMetadataJpaRepository;

  @Mock
  private ForecastMetadataRepository forecastMetadataRepository;

  private static Stream<Arguments> mockArguments() {
    return Stream.of(
        Arguments.of(
            Workflow.FBM_WMS_INBOUND,
            2L,
            mockMetadataView(Workflow.FBM_WMS_INBOUND),
            mockPolyvalenceCardinality(Workflow.FBM_WMS_INBOUND)
        ),
        Arguments.of(
            Workflow.FBM_WMS_OUTBOUND,
            4L,
            mockMetadataView(Workflow.FBM_WMS_OUTBOUND),
            mockPolyvalenceCardinality(Workflow.FBM_WMS_OUTBOUND)
        )
    );
  }

  private static List<ForecastMetadataView> mockMetadataView(final Workflow workflow) {


    return Workflow.FBM_WMS_INBOUND.equals(workflow) ?
        List.of(
            new MetadataView("inbound_receiving_productivity_polyvalences", "80.0"),
            new MetadataView("inbound_putaway_productivity_polivalences", "85.0"),
            new MetadataView("inbound_checkin_productivity_polyvalences", "75.0")
        ) :
        List.of(
            new MetadataView("outbound_batch_sorter_productivity", "80.0"),
            new MetadataView("outbound_packing_productivity", "85.0"),
            new MetadataView("outbound_packing_wall_productivity", "75.0"),
            new MetadataView("outbound_picking_productivity", "90.0"),
            new MetadataView("outbound_wall_in_productivity", "70.0")
        );
  }

  private static List<String> mockPolyvalenceCardinality(final Workflow workflow) {
    return Workflow.FBM_WMS_INBOUND.equals(workflow) ?
        List.of(
            ProductivityPolyvalenceCardinality.RECEIVING_POLYVALENCE.getTagName(),
            ProductivityPolyvalenceCardinality.CHECK_IN_POLYVALENCE.getTagName(),
            ProductivityPolyvalenceCardinality.PUT_AWAY_POLYVALENCE.getTagName()
        ) :
        List.of(
            ProductivityPolyvalenceCardinality.WALL_IN_POLYVALENCE.getTagName(),
            ProductivityPolyvalenceCardinality.PICKING_POLYVALENCE.getTagName(),
            ProductivityPolyvalenceCardinality.PACKING_POLYVALENCE.getTagName(),
            ProductivityPolyvalenceCardinality.PACKING_WALL_POLYVALENCE.getTagName(),
            ProductivityPolyvalenceCardinality.BATCH_SORTER_POLYVALENCE.getTagName()
        );

  }

  @ParameterizedTest
  @MethodSource("mockArguments")
  public void testGetPolyvalencePercentageByWorkflowOk(
      final Workflow workflow,
      final Long forecastId,
      final List<ForecastMetadataView> forecastMetadataViewList,
      final List<String> cardinality) {

    //GIVEN
    when(forecastMetadataRepository.findLastForecastMetadataByWarehouseId(cardinality, List.of(forecastId)))
        .thenReturn(forecastMetadataViewList);

    //WHEN
    PolyvalenceMetadata result = polyvalenceMetadataJpaRepository.getPolyvalencePercentageByWorkflow(forecastId, workflow);

    //THEN
    Assertions.assertNotNull(result);

  }

  @Getter
  @RequiredArgsConstructor
  private static class MetadataView implements ForecastMetadataView {

    private final String key;

    private final String value;
  }
}

