package com.mercadolibre.planning.model.api.domain.usecase.simulation.deactivate;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentProcessingDistributionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeactivateSimulationService {

    private final CurrentProcessingDistributionRepository currentProcessingDistributionRepository;


    public void deactivateSimulation(final DeactivateSimulationOfWeek deactivateSimulationOfWeek) {

        currentProcessingDistributionRepository.deactivateProcessingDistributionForRangeOfDates(
                deactivateSimulationOfWeek.getLogisticCenterId(),
                deactivateSimulationOfWeek.getWorkflow(),
                deactivateSimulationOfWeek.getDateFrom(),
                deactivateSimulationOfWeek.getDateTo(),
                deactivateSimulationOfWeek.getUserId()
        );

    }

}
