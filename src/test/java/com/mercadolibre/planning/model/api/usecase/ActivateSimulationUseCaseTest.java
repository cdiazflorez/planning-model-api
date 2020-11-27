package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentHeadcountProductivityRepository;
import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentHeadcountProductivity;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.ActivateSimulationUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.SimulationInput;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.SimulationOutput;
import com.mercadolibre.planning.model.api.web.controller.request.QuantityByDate;
import com.mercadolibre.planning.model.api.web.controller.simulation.Simulation;
import com.mercadolibre.planning.model.api.web.controller.simulation.SimulationEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.ACTIVE_WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.USER_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.PRODUCTIVITY;
import static java.time.ZonedDateTime.parse;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ActivateSimulationUseCaseTest {

    private static final ZonedDateTime DATE_12 = parse("2020-01-01T12:00:00Z");
    private static final ZonedDateTime DATE_13 = parse("2020-01-01T13:00:00Z");

    @InjectMocks
    private ActivateSimulationUseCase activateSimulationUseCase;

    @Mock
    private CurrentHeadcountProductivityRepository currentProductivityRepository;

    @Mock
    private CurrentProcessingDistributionRepository currentProcessingRepository;

    @Test
    @DisplayName("When creating a new simulation, the old ones turn inactive")
    public void activateSimulationTest() {
        // GIVEN
        final SimulationInput input = mockSimulationInput(List.of(
                new Simulation(PICKING,
                        List.of(new SimulationEntity(HEADCOUNT,
                                List.of(new QuantityByDate(DATE_12, 30),
                                        new QuantityByDate(DATE_13, 25))
                        ))),
                new Simulation(PACKING,
                        singletonList(new SimulationEntity(PRODUCTIVITY,
                                singletonList(new QuantityByDate(DATE_12, 96)))))));

        final Workflow workflow = input.getWorkflow();

        when(currentProcessingRepository.saveAll(any(List.class)))
                .thenReturn(mockCurrentDistribution());

        when(currentProductivityRepository.saveAll(any(List.class)))
                .thenReturn(mockCurrentProductivity());

        // WHEN
        final List<SimulationOutput> simulations = activateSimulationUseCase.execute(input);

        // THEN
        verify(currentProcessingRepository).deactivateProcessingDistribution(input.getWarehouseId(),
                workflow, PICKING, List.of(DATE_12, DATE_13),
                ACTIVE_WORKERS, USER_ID, WORKERS);

        verify(currentProductivityRepository).deactivateProductivity(input.getWarehouseId(),
                workflow, PACKING, singletonList(DATE_12), UNITS_PER_HOUR, USER_ID, 1);

        assertEquals(3, simulations.size());

        final SimulationOutput simulation1 = simulations.get(0);
        assertNull(simulation1.getAbilityLevel());
        assertEquals(DATE_12, simulation1.getDate());
        assertEquals(PICKING, simulation1.getProcessName());
        assertEquals(30, simulation1.getQuantity());
        assertEquals(WORKERS, simulation1.getQuantityMetricUnit());
        assertEquals(workflow, simulation1.getWorkflow());
        assertTrue(simulation1.isActive());

        final SimulationOutput simulation2 = simulations.get(1);
        assertNull(simulation2.getAbilityLevel());
        assertEquals(DATE_13, simulation2.getDate());
        assertEquals(PICKING, simulation2.getProcessName());
        assertEquals(25, simulation2.getQuantity());
        assertEquals(WORKERS, simulation2.getQuantityMetricUnit());
        assertEquals(workflow, simulation2.getWorkflow());
        assertTrue(simulation2.isActive());

        final SimulationOutput simulation3 = simulations.get(2);
        assertEquals(Integer.valueOf(1), simulation3.getAbilityLevel());
        assertEquals(DATE_12, simulation3.getDate());
        assertEquals(PACKING, simulation3.getProcessName());
        assertEquals(96, simulation3.getQuantity());
        assertEquals(UNITS_PER_HOUR, simulation3.getQuantityMetricUnit());
        assertEquals(workflow, simulation3.getWorkflow());
        assertTrue(simulation3.isActive());
    }

    private SimulationInput mockSimulationInput(final List<Simulation> simulations) {
        return SimulationInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .processName(List.of(PICKING, PACKING))
                .dateFrom(A_DATE_UTC)
                .dateTo(A_DATE_UTC.plusDays(1))
                .backlog(singletonList(new QuantityByDate(DATE_12, 1000)))
                .simulations(simulations)
                .userId(USER_ID)
                .build();
    }

    private List<CurrentProcessingDistribution> mockCurrentDistribution() {
        return List.of(
                CurrentProcessingDistribution.builder()
                        .date(DATE_12)
                        .processName(PICKING)
                        .quantity(30)
                        .quantityMetricUnit(WORKERS)
                        .workflow(FBM_WMS_OUTBOUND)
                        .type(ACTIVE_WORKERS)
                        .isActive(true)
                        .build(),
                CurrentProcessingDistribution.builder()
                        .date(DATE_13)
                        .processName(PICKING)
                        .quantity(25)
                        .quantityMetricUnit(WORKERS)
                        .workflow(FBM_WMS_OUTBOUND)
                        .type(ACTIVE_WORKERS)
                        .isActive(true)
                        .build()
        );
    }

    private List<CurrentHeadcountProductivity> mockCurrentProductivity() {
        return singletonList(CurrentHeadcountProductivity.builder()
                .date(DATE_12)
                .processName(PACKING)
                .productivity(96)
                .productivityMetricUnit(UNITS_PER_HOUR)
                .abilityLevel(1)
                .workflow(FBM_WMS_OUTBOUND)
                .isActive(true)
                .build());
    }
}
