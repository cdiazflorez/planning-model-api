package com.mercadolibre.planning.model.api.domain.usecase.forecast.create;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ForecastMetadataRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ForecastRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.HeadcountDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.HeadcountProductivityRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningMetadataRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ForecastMetadata;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountProductivity;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ProcessingDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Service
@AllArgsConstructor
public class CreateForecastUseCase implements UseCase<CreateForecastInput, CreateForecastOutput> {

    private final ForecastRepository forecastRepository;
    private final ForecastMetadataRepository forecastMetadataRepository;
    private final ProcessingDistributionRepository processingDistRepository;
    private final HeadcountDistributionRepository headcountRepository;
    private final HeadcountProductivityRepository productivityRepository;
    private final PlanningDistributionRepository planningRepository;
    private final PlanningMetadataRepository planningMetadataRepository;

    @Override
    @Transactional
    public CreateForecastOutput execute(final CreateForecastInput input) {
        final Forecast savedForecast = forecastRepository.save(toForecast(input));
        final long forecastId = savedForecast.getId();

        forecastMetadataRepository.saveAll(createForecastMetadata(input, forecastId));
        processingDistRepository.saveAll(createProcessingDists(input, savedForecast));
        headcountRepository.saveAll(createHeadcount(input, savedForecast));
        productivityRepository.saveAll(createProductivites(input, savedForecast));
        savePlanningDistributions(input, savedForecast);

        return new CreateForecastOutput(savedForecast.getId());
    }

    private Forecast toForecast(final CreateForecastInput input) {
        return Forecast.builder().workflow(input.getWorkflow()).build();
    }

    private Set<ForecastMetadata> createForecastMetadata(final CreateForecastInput input,
                                                         final long forecastId) {
        return input.getMetadata()
                .stream()
                .map(e -> e.toForecastMetadata(forecastId))
                .collect(toSet());
    }

    private Set<ProcessingDistribution> createProcessingDists(final CreateForecastInput input,
                                                              final Forecast savedForecast) {
        return input.getProcessingDistributions()
                .stream()
                .map(e -> e.toProcessingDistributions(savedForecast))
                .flatMap(Set::stream)
                .collect(toSet());
    }

    private Set<HeadcountDistribution> createHeadcount(final CreateForecastInput input,
                                                       final Forecast savedForecast) {
        return input.getHeadcountDistributions().stream()
                .map(e -> e.toHeadcountDists(savedForecast))
                .flatMap(Set::stream)
                .collect(toSet());

    }

    private Set<HeadcountProductivity> createProductivites(final CreateForecastInput input,
                                                           final Forecast savedForecast) {
        return input.getHeadcountProductivities().stream()
                .map(e -> e.toHeadcountProductivities(savedForecast,
                        input.getPolyvalentProductivities()))
                .flatMap(Set::stream)
                .collect(toSet());

    }

    private void savePlanningDistributions(final CreateForecastInput input,
                                           final Forecast forecast) {
        input.getPlanningDistributions().forEach((pd) -> {
            final PlanningDistribution savedPlanningDist =
                        planningRepository.save(pd.toPlanningDistribution(forecast));

            planningMetadataRepository.saveAll(pd.getMetadata()
                    .stream()
                    .map(mr -> mr.toPlanningDistributionMetadata(savedPlanningDist.getId()))
                    .collect(toSet())
            );
        });
    }
}
