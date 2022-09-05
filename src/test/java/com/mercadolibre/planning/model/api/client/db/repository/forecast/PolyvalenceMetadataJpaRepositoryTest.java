package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import static com.mercadolibre.planning.model.api.domain.entity.ProductivityPolyvalenceCardinality.BATCH_SORTER_POLYVALENCE;
import static com.mercadolibre.planning.model.api.domain.entity.ProductivityPolyvalenceCardinality.CHECK_IN_POLYVALENCE;
import static com.mercadolibre.planning.model.api.domain.entity.ProductivityPolyvalenceCardinality.PACKING_POLYVALENCE;
import static com.mercadolibre.planning.model.api.domain.entity.ProductivityPolyvalenceCardinality.PACKING_WALL_POLYVALENCE;
import static com.mercadolibre.planning.model.api.domain.entity.ProductivityPolyvalenceCardinality.PICKING_POLYVALENCE;
import static com.mercadolibre.planning.model.api.domain.entity.ProductivityPolyvalenceCardinality.PUT_AWAY_POLYVALENCE;
import static com.mercadolibre.planning.model.api.domain.entity.ProductivityPolyvalenceCardinality.RECEIVING_POLYVALENCE;
import static com.mercadolibre.planning.model.api.domain.entity.ProductivityPolyvalenceCardinality.WALL_IN_POLYVALENCE;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.domain.entity.ProductivityPolyvalenceCardinality;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.PolyvalenceMetadata;
import java.util.List;
import java.util.stream.Collectors;
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


    return Workflow.FBM_WMS_INBOUND.equals(workflow)
        ? List.of(
        new MetadataView(RECEIVING_POLYVALENCE.getTagName(), "80.0"),
        new MetadataView(CHECK_IN_POLYVALENCE.getTagName(), "85.0"),
        new MetadataView(PUT_AWAY_POLYVALENCE.getTagName(), "75.0")
    )
        : List.of(
        new MetadataView(BATCH_SORTER_POLYVALENCE.getTagName(), "80.0"),
        new MetadataView(PACKING_POLYVALENCE.getTagName(), "85.0"),
        new MetadataView(PACKING_WALL_POLYVALENCE.getTagName(), "75.0"),
        new MetadataView(PICKING_POLYVALENCE.getTagName(), "90.0"),
        new MetadataView(WALL_IN_POLYVALENCE.getTagName(), "70.0")
    );
  }

  private static List<String> mockPolyvalenceCardinality(final Workflow workflow) {
    return ProductivityPolyvalenceCardinality.getPolyvalencesByWorkflow(workflow).stream()
        .map(ProductivityPolyvalenceCardinality::getTagName)
        .collect(Collectors.toList());

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
    final PolyvalenceMetadata result = polyvalenceMetadataJpaRepository.getPolyvalencePercentageByWorkflow(forecastId, workflow);

    //THEN
    Assertions.assertNotNull(result);
    validatePolyvalences(workflow, result);


  }

  private void validatePolyvalences(final Workflow workflow, final PolyvalenceMetadata result) {

    if (Workflow.FBM_WMS_INBOUND.equals(workflow)) {
      Assertions.assertEquals(80.0F, getValueExpected(result, RECEIVING_POLYVALENCE));
      Assertions.assertEquals(85.0F, getValueExpected(result, CHECK_IN_POLYVALENCE));
      Assertions.assertEquals(75.0F, getValueExpected(result, PUT_AWAY_POLYVALENCE));

    } else {
      Assertions.assertEquals(80.0F, getValueExpected(result, BATCH_SORTER_POLYVALENCE));
      Assertions.assertEquals(85.0F, getValueExpected(result, PACKING_POLYVALENCE));
      Assertions.assertEquals(75.0F, getValueExpected(result, PACKING_WALL_POLYVALENCE));
      Assertions.assertEquals(90.0F, getValueExpected(result, PICKING_POLYVALENCE));
      Assertions.assertEquals(70.0F, getValueExpected(result, WALL_IN_POLYVALENCE));

    }


  }

  private float getValueExpected(final PolyvalenceMetadata polyvalenceMetadata, final ProductivityPolyvalenceCardinality cardinality) {

    return polyvalenceMetadata.getPolyvalences().entrySet().stream()
        .filter(resultPoly -> resultPoly.getKey().equals(cardinality))
        .findAny()
        .get()
        .getValue();
  }

  @Getter
  @RequiredArgsConstructor
  private static class MetadataView implements ForecastMetadataView {

    private final String key;

    private final String value;
  }
}
