package com.mercadolibre.planning.model.api.domain.usecase.unitsdistribution;

import com.mercadolibre.planning.model.api.client.db.repository.metrics.UnitsDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.metrics.UnitsDistribution;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service to handle units-distribution
 */
@Slf4j
@Service
@AllArgsConstructor
public class UnitsDistributionService {

  private final UnitsDistributionRepository unitsDistributionRepository;

  /**
   * Check for the existence c and persist those that are not saved
   *
   * @param unitsInputs List of unitsInput
   * @return List of units-distribution
   */
  public List<UnitsDistribution> save(List<UnitsInput> unitsInputs) {


    ZonedDateTime start = unitsInputs
        .stream()
        .map(UnitsInput::getDate)
        .min(Comparator.naturalOrder())
        .orElseThrow();

    ZonedDateTime end = unitsInputs
        .stream()
        .map(UnitsInput::getDate)
        .max(Comparator.naturalOrder())
        .orElseThrow();


    Set<ZonedDateTime> alreadyStoredDistributions =
        unitsDistributionRepository.findByDateBetweenAndLogisticCenterId(start, end, unitsInputs.get(0).getLogisticCenterId())
            .stream()
            .map(UnitsDistribution::getDate)
            .collect(Collectors.toSet());


    List<UnitsDistribution> unitsDistributionList = unitsInputs
        .stream()
        .filter(unitsInput -> !alreadyStoredDistributions.contains(unitsInput.getDate()))
        .map(u -> new UnitsDistribution(null, u.getLogisticCenterId(), u.getDate(), u.getProcessName(), u.getArea(), u.getQuantity(),
            MetricUnit.of(u.getQuantityMetricUnit()).get(), u.getWorkflow()))
        .collect(Collectors.toList());


    if (!unitsDistributionList.isEmpty()) {
      return unitsDistributionRepository.saveAll(unitsDistributionList);
    }

    return Collections.emptyList();
  }

  /**
   * Obtains a list of units-distribution according to the parameters received
   *
   * @param getUnitsInput Contains warehouse ID, start date and end of search~
   * @return List of units-distribution
   */
  public List<UnitsDistribution> get(GetUnitsInput getUnitsInput) {
    return unitsDistributionRepository.findByDateBetweenAndLogisticCenterId(getUnitsInput.getDateFrom(), getUnitsInput.getDateTo(),
        getUnitsInput.getWareHouseId());
  }


}
