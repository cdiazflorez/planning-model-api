package com.mercadolibre.planning.model.api.domain.usecase.forecast.create;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ForecastMetadata;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountProductivity;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ProcessingDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.deactivate.DeactivateSimulationInput;
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
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import javax.transaction.Transactional;
import java.util.List;


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

    private void saveProcessingDistributions(final CreateForecastInput input,
                                             final Forecast forecast) {

        final List<ProcessingDistributionRequest> distributions =
                input.getProcessingDistributions();

        if (isEmpty(distributions)) {
            return;
        }

        final List<ProcessingDistribution> processingDistributions = distributions.stream()
                        .map(e -> e.toProcessingDistributions(forecast))
                        .flatMap(List::stream).distinct().collect(toList());

        processingDistributionGateway.create(processingDistributions, forecast.getId());
    }

    private void saveHeadcountDistribution(final CreateForecastInput input,
                                           final Forecast forecast) {

        final List<HeadcountDistributionRequest> distributions = input.getHeadcountDistributions();
        if (isEmpty(distributions)) {
            return;
        }

        final List<HeadcountDistribution> headcountDistributions = distributions.stream()
                        .map(e -> e.toHeadcountDists(forecast))
                        .flatMap(List::stream).distinct().collect(toList());

        headcountDistributionGateway.create(headcountDistributions, forecast.getId());
    }

    private void saveHeadcountProductivity(final CreateForecastInput input,
                                           final Forecast forecast) {

        final List<HeadcountProductivityRequest> productivities =
                input.getHeadcountProductivities();

        if (isEmpty(productivities)) {
            return;
        }

        final List<HeadcountProductivity> headcountProductivities = productivities.stream()
                        .map(e -> e.toHeadcountProductivities(
                                forecast,
                                input.getPolyvalentProductivities()))
                        .flatMap(List::stream).distinct().collect(toList());

        headcountProductivityGateway.create(headcountProductivities, forecast.getId());
    }

    private void savePlanningDistributions(final CreateForecastInput input,
                                           final Forecast forecast) {

        final List<PlanningDistributionRequest> distributions = input.getPlanningDistributions();
        if (isEmpty(distributions)) {
            return;
        }

        final List<PlanningDistribution> planningDistributions = distributions.stream()
                .map(pdr -> pdr.toPlanningDistribution(forecast))
                .collect(toList());

        planningDistributionGateway.create(planningDistributions, forecast.getId());
    }

    private void saveBackloglimit(final CreateForecastInput input,
                                   final Forecast forecast) {

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

        final var groupingByDateAndByProcessName = processingDistribution.stream()
                .collect(
                        toMap(
                                ProcessingDistributionRequest::getProcessName,
                                processDistribution -> processDistribution.getData().stream()
                                        .map(ProcessingDistributionDataRequest::getDate)
                                        .distinct()
                                        .collect(toList())
                        )
                );

        List<DeactivateSimulationInput> deactivateSimulationInputs = groupingByDateAndByProcessName.entrySet().stream()
                .map(group -> new DeactivateSimulationInput(
                                logisticCenterId,
                                input.getWorkflow(),
                                group.getKey(),
                                group.getValue(),
                                input.getUserId()
                        )
                ).collect(toList());

        deactivateSimulationService.deactivateSimulation(deactivateSimulationInputs);


    }

    private static <T> boolean isEmpty(List<T> collection) {
        return collection == null || collection.isEmpty();
    }
}
