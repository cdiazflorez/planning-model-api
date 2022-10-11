package com.mercadolibre.planning.model.api.domain.usecase.simulation.deactivate;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentHeadcountProductivityRepository;
import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentProcessingDistributionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeactivateSimulationService {

    private final CurrentProcessingDistributionRepository currentProcessingDistributionRepository;

    private final CurrentHeadcountProductivityRepository currentHeadcountProductivityRepository;


    public void deactivateSimulation(final DeactivateSimulationOfWeek deactivateSimulationOfWeek) {

        currentProcessingDistributionRepository.deactivateProcessingDistributionForRangeOfDates(
                deactivateSimulationOfWeek.getLogisticCenterId(),
                deactivateSimulationOfWeek.getDateFrom(),
                deactivateSimulationOfWeek.getDateTo(),
                deactivateSimulationOfWeek.getUserId()
        );

        currentHeadcountProductivityRepository.deactivateProductivityForRangeOfDates(
                deactivateSimulationOfWeek.getLogisticCenterId(),
                deactivateSimulationOfWeek.getDateFrom(),
                deactivateSimulationOfWeek.getDateTo(),
                deactivateSimulationOfWeek.getUserId()
        );

    }

}
