package com.mercadolibre.planning.model.api.domain.usecase.entities.maxcapacity.get;

import static java.time.ZonedDateTime.parse;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.forecast.MaxCapacityView;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @since FWCO-26.
 *     <p>Model of the responsible for searching the Max capacity by warehouse ID (TPH).
 */
@Service
@AllArgsConstructor
public class GetMaxCapacityByWarehouseEntityUseCase {
  /** Instance of {@link ProcessingDistributionRepository}. */
  private final ProcessingDistributionRepository processingDistRepository;

  /**
   * Executes a TPH search by warehouse and given dates.
   *
   * @param warehouse A {@link String} with the warehouseID.
   * @param dateFrom A {@link ZonedDateTime} with the start date information.
   * @param dateTo A {@link ZonedDateTime} with the end date information.
   * @return A List of {@link MaxCapacityOutput} with all the information of each TPH by warehouseID
   *     from the start date to the end date.
   */
  public List<MaxCapacityOutput> execute(
      final String warehouse, final ZonedDateTime dateFrom, final ZonedDateTime dateTo) {
    final List<MaxCapacityView> maxCapacities =
        processingDistRepository.findMaxCapacitiesByDateInRange(warehouse, null, dateFrom, dateTo);

    return maxCapacities.stream()
        .map(
            item ->
                new MaxCapacityOutput(
                    item.getLogisticCenterId(),
                    parse(item.getLoadDate().toInstant().toString()),
                    parse(item.getMaxCapacityDate().toInstant().toString()),
                    item.getMaxCapacityValue()))
        .collect(Collectors.toList());
  }
}
