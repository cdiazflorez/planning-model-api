package com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.save;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.CurrentForecastDeviationRepository;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import com.mercadolibre.planning.model.api.web.controller.deviation.response.DeviationResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@AllArgsConstructor
public class SaveDeviationUseCase
        implements UseCase<SaveDeviationInput, DeviationResponse> {

    private final CurrentForecastDeviationRepository deviationRepository;

    @Override
    @Transactional
    public DeviationResponse execute(final SaveDeviationInput input) {

        deviationRepository.disableDeviation(input.getWarehouseId(), input.getWorkflow());

        deviationRepository.save(input.toCurrentForecastDeviation());
        return new DeviationResponse(200);
    }


}
