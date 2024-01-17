package com.mercadolibre.planning.model.api.domain.usecase.forecast.create;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ForecastMetadata;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountProductivity;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityEntityUseCase;
import com.mercadolibre.planning.model.api.web.controller.forecast.dto.CreateForecastInputDto;
import com.mercadolibre.planning.model.api.web.controller.forecast.dto.StaffingPlanDto;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.deactivate.DeactivateSimulationOfWeek;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.deactivate.DeactivateSimulationService;
import com.mercadolibre.planning.model.api.gateway.ForecastGateway;
import com.mercadolibre.planning.model.api.gateway.HeadcountDistributionGateway;
import com.mercadolibre.planning.model.api.gateway.HeadcountProductivityGateway;
import com.mercadolibre.planning.model.api.gateway.PlanningDistributionGateway;
import com.mercadolibre.planning.model.api.gateway.ProcessingDistributionGateway;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.HeadcountDistributionRequest;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.PlanningDistributionRequest;
import com.newrelic.api.agent.Trace;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.List;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class CreateForecastUseCase {

  private static final String DATE_KEY = "date";

  private final ForecastGateway forecastGateway;

  private final ProcessingDistributionGateway processingDistributionGateway;

  private final HeadcountDistributionGateway headcountDistributionGateway;

  private final HeadcountProductivityGateway headcountProductivityGateway;

  private final PlanningDistributionGateway planningDistributionGateway;

  private final DeactivateSimulationService deactivateSimulationService;

  @Trace
  @Transactional
  public CreateForecastOutput execute(final CreateForecastInputDto input) {

    deactivateSimulation(input);

    final Forecast forecast = saveForecast(input);

    saveProcessingDistributions(input.staffingPlan(), forecast);
    savePlanningDistributions(input.planningDistributions(), forecast);
    saveHeadcountDistribution(input.headcountDistributions(), forecast);
    //ToDo delete the line below when {@link GetProductivityEntityUseCase} was refactored.
    saveHeadcountProductivity(input.staffingPlan(), forecast);

    return new CreateForecastOutput(forecast.getId());
  }

  private Forecast saveForecast(final CreateForecastInputDto input) {
    final Forecast forecast = Forecast.builder()
        .workflow(input.workflow())
        .logisticCenterId(input.logisticCenterId())
        .week(input.week())
        .userId(input.userId())
        .build();

    final List<ForecastMetadata> metadata = input.metadata().stream()
        .map(metadataRequest -> ForecastMetadata.builder()
            .key(metadataRequest.getKey())
            .value(metadataRequest.getValue())
            .build())
        .toList();

    return forecastGateway.create(forecast, metadata);
  }

  private void saveProcessingDistributions(final List<StaffingPlanDto> input, final Forecast forecast) {
    if (isEmpty(input)) {
      return;
    }
    final var distributions = input.stream().map(sp -> sp.toProcessingDists(forecast)).toList();
    processingDistributionGateway.create(distributions, forecast.getId());
  }

  private void saveHeadcountDistribution(final List<HeadcountDistributionRequest> input, final Forecast forecast) {
    if (isEmpty(input)) {
      return;
    }
    final List<HeadcountDistribution> headcountDistributions = input.stream()
        .map(e -> e.toHeadcountDists(forecast))
        .flatMap(List::stream).distinct().toList();
    headcountDistributionGateway.create(headcountDistributions, forecast.getId());
  }

  /**
   * ToDo delete this method when {@link GetProductivityEntityUseCase} was refactored.
   * @param input Plan staffing list.
   * @param forecast Forecast entity.
   *
   * @deprecated use saveProcessingDistributions method instead.
   */
  @Deprecated
  private void saveHeadcountProductivity(final List<StaffingPlanDto> input, final Forecast forecast) {
    if (isEmpty(input)) {
      return;
    }
    final List<HeadcountProductivity> productivities = input.stream()
        .filter(sp -> sp.type() == ProcessingType.PRODUCTIVITY)
        .map(sp -> sp.toProductivity(forecast))
        .toList();
    headcountProductivityGateway.create(productivities, forecast.getId());
  }

  private void savePlanningDistributions(final List<PlanningDistributionRequest> distributions, final Forecast forecast) {
    if (isEmpty(distributions)) {
      return;
    }

    final List<PlanningDistribution> planningDistributions = distributions.stream()
        .map(pdr -> pdr.toPlanningDistribution(forecast))
        .collect(toMap(CreateForecastUseCase::getGrouperPlanningDistribution, PlanningDistribution::getQuantity, Double::sum)).entrySet()
        .stream()
        .map(item -> new PlanningDistribution(forecast.getId(),
                                              item.getKey().getDateIn(),
                                              item.getKey().getDateOut(),
                                              item.getValue(),
                                              item.getKey().getQuantityMetricUnit(),
                                              item.getKey().getProcessPath(),
                                              forecast,
                                              emptyList()))
        .toList();

    planningDistributionGateway.create(planningDistributions, forecast.getId());
  }

  private void deactivateSimulation(final CreateForecastInputDto input) {

    final List<StaffingPlanDto> staffingPlan = input.staffingPlan();

    if (isEmpty(staffingPlan)) {
      return;
    }

    final ZonedDateTime dateFrom = staffingPlan.stream()
        .map(sp -> ZonedDateTime.parse(sp.tags().get(DATE_KEY)))
        .min(ChronoZonedDateTime::compareTo)
        .get();

    final ZonedDateTime dateTo = staffingPlan.stream()
        .map(sp -> ZonedDateTime.parse(sp.tags().get(DATE_KEY)))
        .max(ChronoZonedDateTime::compareTo)
        .get();

    deactivateSimulationService.deactivateSimulation(
        new DeactivateSimulationOfWeek(input.logisticCenterId(), input.workflow(), dateFrom, dateTo, input.userId())
    );
  }

  private static <T> boolean isEmpty(List<T> collection) {
    return collection == null || collection.isEmpty();
  }

  @AllArgsConstructor
  @Getter
  @EqualsAndHashCode
  private static class GroupPlanningDistribution {
    private ZonedDateTime dateIn;

    private ZonedDateTime dateOut;

    private MetricUnit quantityMetricUnit;

    private ProcessPath processPath;
  }

  private static GroupPlanningDistribution getGrouperPlanningDistribution(final PlanningDistribution planningDistribution) {
    return new GroupPlanningDistribution(planningDistribution.getDateIn(),
                                         planningDistribution.getDateOut(),
                                         planningDistribution.getQuantityMetricUnit(),
                                         planningDistribution.getProcessPath());
  }
}
