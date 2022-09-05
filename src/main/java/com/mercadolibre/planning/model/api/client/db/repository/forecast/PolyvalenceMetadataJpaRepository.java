package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.ProductivityPolyvalenceCardinality;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetPolyvalenceForecastMetadata;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.PolyvalenceMetadata;
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

    final List<ProductivityPolyvalenceCardinality> cardinality = ProductivityPolyvalenceCardinality.getPolyvalencesByWorkflow(workflow);

    final List<ForecastMetadataView> polyvalencesMetadata =
        forecastMetadataRepository.findLastForecastMetadataByWarehouseId(
            cardinality.stream()
                .map(ProductivityPolyvalenceCardinality::getTagName)
                .collect(Collectors.toList()),
            List.of(forecastId));

    final Map<ProductivityPolyvalenceCardinality, Float> polyvalences = polyvalencesMetadata.stream()
        .collect(Collectors
            .toMap(
                forecastMetadataView -> cardinality
                    .stream()
                    .filter(polyvalence -> polyvalence.getTagName().equals(forecastMetadataView.getKey()))
                    .findFirst()
                    .get(),
                forecastMetadataView -> getPolyvalenceValue(forecastMetadataView.getValue())
            )
        );

    return new PolyvalenceMetadata(polyvalences);
  }

  private float getPolyvalenceValue(final String value) {
    return Float.parseFloat(value);
  }
}
