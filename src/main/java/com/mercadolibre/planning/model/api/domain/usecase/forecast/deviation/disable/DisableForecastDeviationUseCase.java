package com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.disable;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.CurrentForecastDeviationRepository;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

import java.util.List;

import static java.time.ZonedDateTime.now;

@Service
@AllArgsConstructor
public class DisableForecastDeviationUseCase
        implements UseCase<DisableForecastDeviationInput, Integer> {
    private final CurrentForecastDeviationRepository deviationRepository;

    @Override
    @Transactional
    public Integer execute(final DisableForecastDeviationInput input) {
        final List<CurrentForecastDeviation> warehouseDeviations =
                deviationRepository.findByLogisticCenterId(input.getWarehouseId());

        warehouseDeviations.forEach(
                currentForecastDeviation -> {
                    currentForecastDeviation.setActive(false);
                    currentForecastDeviation.setLastUpdated(now());
                }
        );

        deviationRepository.saveAll(warehouseDeviations);

        return warehouseDeviations.size();
    }


}
