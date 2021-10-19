package com.mercadolibre.planning.model.api.domain.usecase.suggestedwave.get;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ForecastMetadataView;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionView;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.SuggestedWavePlanningDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.WaveCardinality;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.search.SearchEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastMetadataInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastMetadataUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.newrelic.api.agent.Trace;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.REMAINING_PROCESSING;
import static java.time.format.DateTimeFormatter.ISO_TIME;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@AllArgsConstructor
public class GetSuggestedWavesUseCase
        implements UseCase<GetSuggestedWavesInput, List<SuggestedWavesOutput>> {

    private final SearchEntityUseCase searchEntityUseCase;
    private final GetForecastMetadataUseCase getForecastMetadataUseCase;
    private final GetForecastUseCase getForecastUseCase;
    private final PlanningDistributionRepository planningDistRepository;
    private final ProcessingDistributionRepository processingDistRepository;

    private static final long HOUR_IN_MINUTES = 60L;

    @Trace
    @Override
    public List<SuggestedWavesOutput> execute(final GetSuggestedWavesInput input) {
        final List<Long> forecastIds = getForecastUseCase.execute(GetForecastInput.builder()
                        .workflow(input.getWorkflow())
                        .warehouseId(input.getWarehouseId())
                        .dateFrom(input.getDateFrom())
                        .dateTo(input.getDateTo())
                        .build());

        final long initialBacklog = input.getBacklog();
        final long sales = getBoundedSales(input, forecastIds);
        final double remainingProcessing = (double) getRemainingProcessing(input) / HOUR_IN_MINUTES;
        final long capex = getCapex(input, forecastIds);
        final long suggestedWavingUnits =
                (long) Math.floor((initialBacklog + sales) / (1 + remainingProcessing));

        logCalculationsInfo(input, initialBacklog, sales, remainingProcessing, capex);

        final long unitsToWave = Math.min(suggestedWavingUnits, Math.min(capex, initialBacklog));

        final List<ForecastMetadataView> forecastMetadataPercentage = getForecastMetadataUseCase
                .execute(GetForecastMetadataInput.builder()
                        .forecastIds(forecastIds)
                        .dateFrom(input.getDateFrom())
                        .dateTo(input.getDateTo())
                        .build()
                );

        return forecastMetadataPercentage.stream()
                .map(fm -> new SuggestedWavesOutput(
                        WaveCardinality.of(fm.getKey()).orElse(null),
                        calculateSuggestedWave(unitsToWave,Float.parseFloat(fm.getValue()))))
                .collect(toList());
    }

    private void logCalculationsInfo(final GetSuggestedWavesInput input,
                                     final long initialBacklog,
                                     final long sales,
                                     final double remainingProcessing,
                                     final long capex) {
        final String timeFrom = input.getDateFrom().truncatedTo(MINUTES).format(ISO_TIME);
        final String timeTo = input.getDateTo().truncatedTo(MINUTES).format(ISO_TIME);

        log.info("Calculating suggested waves for warehouse {} in period ({} - {})\n"
                + "Current backlog: {}\n"
                + "Sales for period: {}\n"
                + "Healthy backlog after period (in hours): {}\n"
                + "CAPEX (in units per hour): {}\n",
                input.getWarehouseId(), timeFrom, timeTo, initialBacklog,
                sales, remainingProcessing, capex
        );
    }

    private long getCapex(final GetSuggestedWavesInput input, final List<Long> forecastIds) {
        final List<ProcessingDistributionView> processingDistributionView =
                processingDistRepository
                        .findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                                Set.of(ProcessingType.MAX_CAPACITY.name()),
                                List.of(ProcessName.GLOBAL.toJson()),
                                input.getDateTo().minusHours(1),
                                input.getDateTo().minusHours(1),
                                forecastIds
                        );

        return processingDistributionView.stream()
                .findFirst()
                .map(ProcessingDistributionView::getQuantity)
                .orElse(0L);
    }

    private long calculateSuggestedWave(final Long waveSuggest, final float percentage) {
        return (long) Math.floor((percentage / 100) * waveSuggest);
    }

    private long getRemainingProcessing(final GetSuggestedWavesInput input) {
        return searchEntityUseCase.execute(GetEntityInput.builder()
                .workflow(input.getWorkflow())
                .processName(List.of(PICKING))
                .warehouseId(input.getWarehouseId())
                .entityType(REMAINING_PROCESSING)
                .dateFrom(input.getDateTo().minusHours(1))
                .dateTo(input.getDateTo().minusHours(1))
                .build()
        ).stream().findFirst()
                .map(EntityOutput::getValue)
                .orElse(0L);
    }

    private long getBoundedSales(final GetSuggestedWavesInput input, final List<Long> forecastIds) {
        final ZonedDateTime dateFrom = input.getDateFrom();

        final SuggestedWavePlanningDistributionView currentHourSales = planningDistRepository
                .findByWarehouseIdWorkflowDateInRange(
                        input.getWarehouseId(),
                        dateFrom.truncatedTo(HOURS),
                        dateFrom.truncatedTo(HOURS).plusHours(1).minusMinutes(1),
                        forecastIds,
                        input.isApplyDeviation());

        long totalSales = currentHourSales == null ? 0
                : currentHourSalesPercentage(currentHourSales.getQuantity(), dateFrom);

        if (dateFrom.truncatedTo(HOURS).plusHours(1).isBefore(input.getDateTo())) {
            final SuggestedWavePlanningDistributionView nextHourSales = planningDistRepository
                    .findByWarehouseIdWorkflowDateInRange(
                            input.getWarehouseId(),
                            dateFrom.truncatedTo(HOURS).plusHours(1),
                            input.getDateTo().minusMinutes(1),
                            forecastIds,
                            input.isApplyDeviation());

            totalSales += nextHourSales == null ? 0 : nextHourSales.getQuantity();
        }

        return totalSales;
    }

    private long currentHourSalesPercentage(final long currentHourSales,
                                            final ZonedDateTime now) {
        return  currentHourSales * (HOUR_IN_MINUTES - now.withFixedOffsetZone().getMinute())
                / HOUR_IN_MINUTES;
    }
}
