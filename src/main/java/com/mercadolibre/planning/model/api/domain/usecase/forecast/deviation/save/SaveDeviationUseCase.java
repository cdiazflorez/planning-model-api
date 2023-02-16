package com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.save;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.CurrentForecastDeviationRepository;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import com.mercadolibre.planning.model.api.web.controller.deviation.response.DeviationResponse;
import java.util.List;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class SaveDeviationUseCase {

    private final CurrentForecastDeviationRepository deviationRepository;

    @Transactional
    public DeviationResponse execute(final List<SaveDeviationInput> inputs) {
        inputs.forEach(this::saveDeviations);
        return new DeviationResponse(200);
    }

    private void saveDeviations(final SaveDeviationInput input) {
        final List<CurrentForecastDeviation> forecastDeviations = input.getPaths() != null
            ? input.getPaths().stream()
            .map(path -> CurrentForecastDeviation
                .builder()
                .logisticCenterId(input.getWarehouseId())
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .value(input.getValue())
                .isActive(true)
                .userId(input.getUserId())
                .workflow(input.getWorkflow())
                .type(input.getDeviationType())
                .path(path)
                .build())
            .collect(Collectors.toList())
            : List.of(input.toCurrentForecastDeviation());

        deviationRepository.disableDeviation(input.getWarehouseId(), input.getWorkflow(), input.getDeviationType(), input.getPaths());
        deviationRepository.saveAll(forecastDeviations);
    }
}
