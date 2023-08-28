package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.MetadataCardinality;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastMetadata.MetadataRepository;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.Metadata;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class MetadataJpaRepository implements MetadataRepository {
  private ForecastMetadataRepository forecastMetadataRepository;

  @Override
  public List<Metadata> getMetadataByTag(final Long forecastId) {

    final List<String> cardinality = MetadataCardinality.getMetadataTag();

    final List<ForecastMetadataView> metadata =
        forecastMetadataRepository.findForecastMetadata(
            new ArrayList<>(cardinality),
            List.of(forecastId));

    return metadata.stream()
        .map(r -> new Metadata(r.getKey(), r.getValue()))
        .collect(Collectors.toList());
  }
}
