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


  /**
   * Get last forecast from repository by workflow.
   */
  public interface LastForecastRepository {

    /**
     * Get last forecastId of week by workflow.
     *
     * @param logisticCenterId logistic center id.
     * @param workflow         workflow
     * @param week             week of received day
     * @return ForecastId.
     */
    Long getForecastByWorkflow(String logisticCenterId, Workflow workflow, String week);

  }

  /**
   * Get polyvalence percentage metadata of process (stage) from repository
   */
  public interface PolyvalenceMetadataRepository {

    /**
     * Get polyvalence percentage of process (stages) from forecast_metadata by forecast id and workflow.
     *
     * @param forecastId ForecastId
     * @param workflow   workflow
     * @return PolyvalenceMetadata - percentage polyvalences of process (stages) by workflow.
     */
    PolyvalenceMetadata getPolyvalencePercentageByWorkflow(Long forecastId, Workflow workflow);

  }
}
