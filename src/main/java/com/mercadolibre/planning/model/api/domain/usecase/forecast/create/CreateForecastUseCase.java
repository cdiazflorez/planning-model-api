package com.mercadolibre.planning.model.api.domain.usecase.forecast.create;

import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ForecastMetadata;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountProductivity;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ProcessingDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import com.mercadolibre.planning.model.api.gateway.ForecastGateway;
import com.mercadolibre.planning.model.api.gateway.HeadcountDistributionGateway;
import com.mercadolibre.planning.model.api.gateway.HeadcountProductivityGateway;
import com.mercadolibre.planning.model.api.gateway.PlanningDistributionGateway;
import com.mercadolibre.planning.model.api.gateway.ProcessingDistributionGateway;
import com.newrelic.api.agent.Trace;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

import java.util.List;

import static java.util.stream.Collectors.toList;


@Service
@AllArgsConstructor
public class CreateForecastUseCase implements UseCase<CreateForecastInput, CreateForecastOutput> {

    private final ForecastGateway forecastGateway;

    private final ProcessingDistributionGateway processingDistributionGateway;

    private final HeadcountDistributionGateway headcountDistributionGateway;

    private final HeadcountProductivityGateway headcountProductivityGateway;

    private final PlanningDistributionGateway planningDistributionGateway;

    @Trace
    @Override
    @Transactional
    public CreateForecastOutput execute(final CreateForecastInput input) {

        final Forecast forecast = saveForecast(input);

        saveProcessingDistributions(input, forecast);
        saveHeadcountDistribution(input, forecast);
        saveHeadcountProductivity(input, forecast);
        savePlanningDistributions(input, forecast);
        saveBackloglimit(input, forecast);

        return new CreateForecastOutput(forecast.getId());
    }

    private Forecast saveForecast(final CreateForecastInput input) {
        final Forecast forecast = Forecast.builder().workflow(input.getWorkflow()).build();
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

        final List<ProcessingDistribution> processingDistributions =
                input.getProcessingDistributions().stream()
                        .map(e -> e.toProcessingDistributions(forecast))
                        .flatMap(List::stream).distinct().collect(toList());

        processingDistributionGateway.create(processingDistributions, forecast.getId());
    }

    private void saveHeadcountDistribution(final CreateForecastInput input,
                                           final Forecast forecast) {

        final List<HeadcountDistribution> headcountDistributions =
                input.getHeadcountDistributions().stream()
                        .map(e -> e.toHeadcountDists(forecast))
                        .flatMap(List::stream).distinct().collect(toList());

        headcountDistributionGateway.create(headcountDistributions, forecast.getId());
    }

    private void saveHeadcountProductivity(final CreateForecastInput input,
                                           final Forecast forecast) {

        final List<HeadcountProductivity> headcountProductivities =
                input.getHeadcountProductivities().stream()
                        .map(e -> e.toHeadcountProductivities(
                                forecast,
                                input.getPolyvalentProductivities()))
                        .flatMap(List::stream).distinct().collect(toList());

        headcountProductivityGateway.create(headcountProductivities, forecast.getId());
    }

    private void savePlanningDistributions(final CreateForecastInput input,
                                           final Forecast forecast) {

        final List<PlanningDistribution> pDistributions = input.getPlanningDistributions().stream()
                .map(pdr -> pdr.toPlanningDistribution(forecast))
                .collect(toList());

        planningDistributionGateway.create(pDistributions, forecast.getId());
    }

    private  void saveBackloglimit(final CreateForecastInput input,
                                   final Forecast forecast) {

        final List<ProcessingDistribution> backlogList =
                input.getBacklogLimits().stream()
                        .map(e -> e.toProcessingDistributions(forecast))
                        .flatMap(List::stream).distinct().collect(toList());

        processingDistributionGateway.create(backlogList, forecast.getId());


    }
}
