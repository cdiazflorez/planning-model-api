package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.domain.entity.MetadataCardinality;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.Metadata;
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
public class MetadataJpaRepositoryTest {
  @InjectMocks
  private MetadataJpaRepository metadataJpaRepository;
  @Mock
  private ForecastMetadataRepository forecastMetadataRepository;

  private static Stream<Arguments> mockArguments() {
    return Stream.of(
        Arguments.of(
            4L,
            mockMetadata(),
            mockCardinality()
        )
    );
  }

  private static List<ForecastMetadataView> mockMetadata() {
    return List.of(
        new MetadataView("week", "11-2023"),
        new MetadataView("warehouse_id", "ARTW01"),
        new MetadataView("version", "2.0"),
        new MetadataView("units_per_order_ratio", "3.96"),
        new MetadataView("outbound_wall_in_productivity", "100"),
        new MetadataView("outbound_picking_productivity", "80"),
        new MetadataView("outbound_packing_wall_productivity", "90"),
        new MetadataView("outbound_packing_productivity", "100"),
        new MetadataView("outbound_batch_sorter_productivity", "100"),
        new MetadataView("multi_order_distribution", "26"),
        new MetadataView("multi_batch_distribution", "32"),
        new MetadataView("mono_order_distribution", "42")
    );
  }

  private static List<String> mockCardinality() {
    return MetadataCardinality.getMetadataTag();
  }

  @ParameterizedTest
  @MethodSource("mockArguments")
  public void testGetMetadataByTagOk(
      final Long forecastId,
      final List<ForecastMetadataView> forecastMetadataViewList,
      final List<String> cardinality) {

    //GIVEN
    when(forecastMetadataRepository.findLastForecastMetadataByWarehouseId(cardinality, List.of(forecastId)))
        .thenReturn(forecastMetadataViewList);

    //WHEN
    final List<Metadata> result = metadataJpaRepository.getMetadataByTag(forecastId);

    //THEN
    Assertions.assertNotNull(result);
    Assertions.assertEquals(12, result.size());
  }

  @Getter
  @RequiredArgsConstructor
  private static class MetadataView implements ForecastMetadataView {

    private final String key;

    private final String value;
  }
}
