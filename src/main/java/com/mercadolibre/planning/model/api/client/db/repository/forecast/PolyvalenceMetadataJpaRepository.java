package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.ProductivityPolyvalenceCardinality;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetPolyvalenceForecastMetadata;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.PolyvalenceMetadata;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PolyvalenceMetadataJpaRepository implements GetPolyvalenceForecastMetadata.PolyvalenceMetadataRepository {

  private ForecastMetadataRepository forecastMetadataRepository;

  @Override
  public PolyvalenceMetadata getPolyvalencePercentageByWorkflow(final Long forecastId, final Workflow workflow) {
    final List<String> cardinality = getPolyvalenceCardinality().get(workflow);
    List<ForecastMetadataView> polyvalencesMetadata =
        forecastMetadataRepository.findLastForecastMetadataByWarehouseId(cardinality, List.of(forecastId));

    final Map<ProductivityPolyvalenceCardinality, Float> polyvalences = polyvalencesMetadata.stream()
        .collect(Collectors
            .toMap(forecastMetadataView -> getPolyvalenceKey(forecastMetadataView.getKey()),
                forecastMetadataView -> getPolyvalenceValue(forecastMetadataView.getValue()))
        );

    return new PolyvalenceMetadata(polyvalences);
  }

  private Map<Workflow, List<String>> getPolyvalenceCardinality() {
    return Map.of(Workflow.FBM_WMS_INBOUND,
        List.of(
            ProductivityPolyvalenceCardinality.RECEIVING_POLYVALENCE.getTagName(),
            ProductivityPolyvalenceCardinality.CHECK_IN_POLYVALENCE.getTagName(),
            ProductivityPolyvalenceCardinality.PUT_AWAY_POLYVALENCE.getTagName()
        ),
        Workflow.FBM_WMS_OUTBOUND,
        List.of(
            ProductivityPolyvalenceCardinality.WALL_IN_POLYVALENCE.getTagName(),
            ProductivityPolyvalenceCardinality.PICKING_POLYVALENCE.getTagName(),
            ProductivityPolyvalenceCardinality.PACKING_POLYVALENCE.getTagName(),
            ProductivityPolyvalenceCardinality.PACKING_WALL_POLYVALENCE.getTagName(),
            ProductivityPolyvalenceCardinality.BATCH_SORTER_POLYVALENCE.getTagName()
        ));
  }

  private ProductivityPolyvalenceCardinality getPolyvalenceKey(final String key) {
    return Arrays.stream(ProductivityPolyvalenceCardinality.values())
        .filter(cardinality -> cardinality.getTagName().equals(key))
        .findAny()
        .get();
  }

  private Float getPolyvalenceValue(final String value) {
    return Float.parseFloat(value);
  }
}
