package com.mercadolibre.planning.model.api.domain.usecase.forecast.create;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ForecastMetadata;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountProductivity;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ProcessingDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.deactivate.DeactivateSimulationOfWeek;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.deactivate.DeactivateSimulationService;
import com.mercadolibre.planning.model.api.gateway.ForecastGateway;
import com.mercadolibre.planning.model.api.gateway.HeadcountDistributionGateway;
import com.mercadolibre.planning.model.api.gateway.HeadcountProductivityGateway;
import com.mercadolibre.planning.model.api.gateway.PlanningDistributionGateway;
import com.mercadolibre.planning.model.api.gateway.ProcessingDistributionGateway;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.HeadcountDistributionRequest;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.HeadcountProductivityRequest;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.MetadataRequest;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.PlanningDistributionRequest;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.ProcessingDistributionDataRequest;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.ProcessingDistributionRequest;
import com.newrelic.api.agent.Trace;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.List;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CreateForecastUseCase {

  private static final String WAREHOUSE_ID = "warehouse_id";

  private final ForecastGateway forecastGateway;

  private final ProcessingDistributionGateway processingDistributionGateway;

  private final HeadcountDistributionGateway headcountDistributionGateway;

  private final HeadcountProductivityGateway headcountProductivityGateway;

  private final PlanningDistributionGateway planningDistributionGateway;

  private final DeactivateSimulationService deactivateSimulationService;

  @Trace
  @Transactional
  public CreateForecastOutput execute(final CreateForecastInput input) {

    deactivateSimulation(input);

    final Forecast forecast = saveForecast(input);

    saveProcessingDistributions(input, forecast);
    saveHeadcountDistribution(input, forecast);
    saveHeadcountProductivity(input, forecast);
    savePlanningDistributions(input, forecast);
    saveBackloglimit(input, forecast);

    return new CreateForecastOutput(forecast.getId());
  }

  private Forecast saveForecast(final CreateForecastInput input) {
    final Forecast forecast = Forecast.builder()
        .userId(input.getUserId())
        .workflow(input.getWorkflow()).build();

    final List<ForecastMetadata> metadata = input.getMetadata().stream()
        .map(metadataRequest -> ForecastMetadata.builder()
            .key(metadataRequest.getKey())
            .value(metadataRequest.getValue())
            .build())
        .collect(toList());

    return forecastGateway.create(forecast, metadata);
  }

  private void saveProcessingDistributions(final CreateForecastInput input, final Forecast forecast) {
    final List<ProcessingDistributionRequest> distributions = input.getProcessingDistributions();

    if (isEmpty(distributions)) {
      return;
    }

    final List<ProcessingDistribution> processingDistributions = distributions.stream()
        .map(e -> e.toProcessingDistributions(forecast))
        .flatMap(List::stream).distinct().collect(toList());

    processingDistributionGateway.create(processingDistributions, forecast.getId());
  }

  private void saveHeadcountDistribution(final CreateForecastInput input, final Forecast forecast) {
    final List<HeadcountDistributionRequest> distributions = input.getHeadcountDistributions();
    if (isEmpty(distributions)) {
      return;
    }

    final List<HeadcountDistribution> headcountDistributions = distributions.stream()
        .map(e -> e.toHeadcountDists(forecast))
        .flatMap(List::stream).distinct().collect(toList());

    headcountDistributionGateway.create(headcountDistributions, forecast.getId());
  }

  private void saveHeadcountProductivity(final CreateForecastInput input, final Forecast forecast) {
    final List<HeadcountProductivityRequest> productivities = input.getHeadcountProductivities();
    if (isEmpty(productivities)) {
      return;
    }

    final List<HeadcountProductivity> headcountProductivities = productivities.stream()
        .map(e -> e.toHeadcountProductivities(forecast, input.getPolyvalentProductivities()))
        .flatMap(List::stream).distinct().collect(toList());

    headcountProductivityGateway.create(headcountProductivities, forecast.getId());
  }

  private void savePlanningDistributions(final CreateForecastInput input, final Forecast forecast) {
    final List<PlanningDistributionRequest> distributions = input.getPlanningDistributions();
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
        .collect(toList());

    planningDistributionGateway.create(planningDistributions, forecast.getId());
  }

  private void saveBackloglimit(final CreateForecastInput input, final Forecast forecast) {
    final List<ProcessingDistributionRequest> limits = input.getBacklogLimits();
    if (isEmpty(limits)) {
      return;
    }

    final List<ProcessingDistribution> backlogList = limits.stream()
        .map(e -> e.toProcessingDistributions(forecast))
        .flatMap(List::stream).distinct().collect(toList());

    processingDistributionGateway.create(backlogList, forecast.getId());
  }

  private void deactivateSimulation(final CreateForecastInput input) {

    final List<ProcessingDistributionRequest> processingDistribution = input.getProcessingDistributions();

    if (isEmpty(processingDistribution)) {
      return;
    }

    final String logisticCenterId = input.getMetadata().stream()
        .filter(metadataRequest -> WAREHOUSE_ID.equals(metadataRequest.getKey()))
        .map(MetadataRequest::getValue)
        .findFirst().orElseThrow();

    final ZonedDateTime dateFrom = processingDistribution.stream()
        .map(ProcessingDistributionRequest::getData)
        .map(processingDistributionDataRequests -> processingDistributionDataRequests.stream()
            .map(ProcessingDistributionDataRequest::getDate)
            .min(ChronoZonedDateTime::compareTo)
            .get())
        .min(ChronoZonedDateTime::compareTo)
        .get();

    final ZonedDateTime dateTo = processingDistribution.stream()
        .map(ProcessingDistributionRequest::getData)
        .map(processingDistributionDataRequests -> processingDistributionDataRequests.stream()
            .map(ProcessingDistributionDataRequest::getDate)
            .max(ChronoZonedDateTime::compareTo)
            .get())
        .max(ChronoZonedDateTime::compareTo)
        .get();

    deactivateSimulationService.deactivateSimulation(
        new DeactivateSimulationOfWeek(logisticCenterId, dateFrom, dateTo, input.getUserId())
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
