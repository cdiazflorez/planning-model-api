package com.mercadolibre.planning.model.api.domain.usecase.forecast.get;

import static com.mercadolibre.planning.model.api.util.DateUtils.getActualForecastWeek;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class GetPolyvalenceForecastMetadata {

  private LastForecastRepository lastForecastRepository;
  private PolyvalenceMetadataRepository polyvalenceMetadataRepository;


  public PolyvalenceMetadata getPolyvalencePercentage(final String warehouseId,
                                                      final Workflow workflow,
                                                      final ZonedDateTime dateTime) {

    final String actualForecastWeek = getActualForecastWeek(dateTime);

    final Long forecastId = lastForecastRepository.getForecastByWorkflow(warehouseId, workflow, actualForecastWeek);

    return polyvalenceMetadataRepository.getPolyvalencePercentageByWorkflow(forecastId, workflow);

  }


  public interface LastForecastRepository {

    Long getForecastByWorkflow(String logisticCenterId, Workflow workflow, String week);

  }

  public interface PolyvalenceMetadataRepository {

    PolyvalenceMetadata getPolyvalencePercentageByWorkflow(Long forecastId, Workflow workflow);

  }
}
