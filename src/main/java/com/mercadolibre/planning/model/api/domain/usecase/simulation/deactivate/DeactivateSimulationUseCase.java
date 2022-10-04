package com.mercadolibre.planning.model.api.domain.usecase.simulation.deactivate;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.ACTIVE_WORKERS;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.PRODUCTIVITY;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentHeadcountProductivityRepository;
import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeactivateSimulationUseCase {

    private static final int ORIGINAL_WORKER_ABILITY = 1;
    private final CurrentProcessingDistributionRepository currentProcessingDistributionRepository;

    private final CurrentHeadcountProductivityRepository currentHeadcountProductivityRepository;


    public void deactivateSimulation(final List<DeactivateSimulationInput> deactivateSimulationInputs) {

        deactivateSimulationInputs
                .forEach(
                deactivateSimulationInput -> currentProcessingDistributionRepository.deactivateProcessingDistribution(
                        deactivateSimulationInput.getLogisticCenterId(),
                        deactivateSimulationInput.getWorkflow(),
                        deactivateSimulationInput.getProcessName(),
                        deactivateSimulationInput.getDates(),
                        ACTIVE_WORKERS,
                        deactivateSimulationInput.getUserId(),
                        WORKERS
                )
        );

        deactivateSimulationInputs
                .forEach(
                deactivateSimulationInput -> currentHeadcountProductivityRepository.deactivateProductivity(
                        deactivateSimulationInput.getLogisticCenterId(),
                        deactivateSimulationInput.getWorkflow(),
                        deactivateSimulationInput.getProcessName(),
                        deactivateSimulationInput.getDates(),
                        UNITS_PER_HOUR,
                        deactivateSimulationInput.getUserId(),
                        ORIGINAL_WORKER_ABILITY
                )
        );

    }

}
