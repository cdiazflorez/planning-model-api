package com.mercadolibre.planning.model.api.domain.usecase.forecast.get;

import static com.mercadolibre.planning.model.api.util.DateUtils.getActualForecastWeek;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class GetForecastMetadata {
  private LastForecastRepository lastForecastRepository;
  private MetadataRepository metadataRepository;


  public List<Metadata> getMetadata(final String warehouseId,
                                    final Workflow workflow,
                                    final ZonedDateTime dateTime) {

    final String actualForecastWeek = getActualForecastWeek(dateTime);

    final Long forecastId = lastForecastRepository.getForecastByWorkflow(warehouseId, workflow, actualForecastWeek);

    return metadataRepository.getMetadataByTag(forecastId);
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
   * Get metadata of process (stage) from repository.
   */
  public interface MetadataRepository {

    /**
     * Get process (stages) from forecast_metadata by forecast id.
     *
     * @param forecastId ForecastId
     * @return ForecastMetadataView - process (stages) by workflow.
     */
    List<Metadata> getMetadataByTag(Long forecastId);
  }
}
