package com.mercadolibre.planning.model.api.usecase.simulation.activate;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.GLOBAL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.EFFECTIVE_WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.USER_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.MAX_CAPACITY;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.PRODUCTIVITY;
import static java.time.ZonedDateTime.parse;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingDistribution;
import com.mercadolibre.planning.model.api.domain.service.headcount.ProcessPathHeadcountShareService;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.activate.ActivateSimulationUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.activate.SimulationInput;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.activate.SimulationOutput;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import com.mercadolibre.planning.model.api.web.controller.projection.request.QuantityByDate;
import com.mercadolibre.planning.model.api.web.controller.simulation.Simulation;
import com.mercadolibre.planning.model.api.web.controller.simulation.SimulationEntity;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ActivateSimulationUseCaseTest {

  private static final ZonedDateTime DATE_12 = parse("2020-01-01T12:00:00Z");
  private static final ZonedDateTime DATE_13 = parse("2020-01-01T13:00:00Z");

  @InjectMocks
  private ActivateSimulationUseCase activateSimulationUseCase;

  @Mock
  private CurrentProcessingDistributionRepository currentProcessingRepository;

  @Mock
  private ProcessPathHeadcountShareService processPathHeadcountShareService;

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
                singletonList(new QuantityByDate(DATE_12, 96))))),
        new Simulation(GLOBAL,
            singletonList(new SimulationEntity(MAX_CAPACITY,
                singletonList(new QuantityByDate(DATE_12, 96)))))
    ));

    final Workflow workflow = input.getWorkflow();

    final Map<ProcessPath, Map<ProcessName, Map<Instant, Double>>> processPathHeadcountResponse = Map.of(
            ProcessPath.GLOBAL,
            Map.of(
                    PICKING,
                    Map.of(
                            DATE_12.toInstant(), 1.0D,
                            DATE_13.toInstant(), 1.0D
                    )
            )
    );

    when(processPathHeadcountShareService.getHeadcountShareByProcessPath(anyString(), any(), anySet(), any(), any(), any()))
            .thenReturn(processPathHeadcountResponse);

    when(currentProcessingRepository.saveAll(anyList()))
        .thenReturn(mockCurrentDistribution());

    // WHEN
    final List<SimulationOutput> simulations = activateSimulationUseCase.execute(input);

    // THEN
    verify(currentProcessingRepository).deactivateProcessingDistribution(input.getWarehouseId(),
        workflow, PICKING, List.of(DATE_12, DATE_13),
        EFFECTIVE_WORKERS, USER_ID, WORKERS);

    verify(currentProcessingRepository).deactivateProcessingDistribution(input.getWarehouseId(),
        workflow, PACKING, singletonList(DATE_12), ProcessingType.PRODUCTIVITY, USER_ID, UNITS_PER_HOUR);

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
    assertEquals(DATE_12, simulation3.getDate());
    assertEquals(PACKING, simulation3.getProcessName());
    assertEquals(96, simulation3.getQuantity());
    assertEquals(UNITS_PER_HOUR, simulation3.getQuantityMetricUnit());
    assertEquals(workflow, simulation3.getWorkflow());
    assertTrue(simulation3.isActive());
  }


  @Test
  @DisplayName("Create a new simulation with process paths active")
  public void activateSimulationWithProcessPathTest() {
    //GIVEN
    final SimulationInput input = mockSimulationInput(
            List.of(
                    mockSimulation(PICKING, HEADCOUNT, 15),
                    mockSimulation(PICKING, PRODUCTIVITY, 30),
                    mockSimulation(PACKING, HEADCOUNT, 5),
                    mockSimulation(PACKING, PRODUCTIVITY, 35),
                    mockSimulation(PACKING_WALL, HEADCOUNT, 10),
                    mockSimulation(PACKING_WALL, PRODUCTIVITY, 15)
            )
    );

    final Map<ProcessPath, Map<ProcessName, Map<Instant, Double>>> processPathHeadcountResponse = mockProcessPathHeadcountShare();

    final List<CurrentProcessingDistribution> currentProcessingDistributions = List.of(
            mockCurrentProcessingDistribution(DATE_12, PICKING, ProcessPath.GLOBAL, 15),
            mockCurrentProcessingDistribution(DATE_12, PICKING, ProcessPath.TOT_MULTI_ORDER, 15),
            mockCurrentProcessingDistribution(DATE_12, PICKING, ProcessPath.NON_TOT_MULTI_ORDER, 15),
            mockCurrentProcessingDistribution(DATE_13, PICKING, ProcessPath.GLOBAL, 15),
            mockCurrentProcessingDistribution(DATE_13, PICKING, ProcessPath.TOT_MULTI_ORDER, 15),
            mockCurrentProcessingDistribution(DATE_13, PICKING, ProcessPath.NON_TOT_MULTI_ORDER, 15),
            mockCurrentProcessingDistribution(DATE_12, PACKING, ProcessPath.GLOBAL, 5),
            mockCurrentProcessingDistribution(DATE_13, PACKING, ProcessPath.GLOBAL, 5),
            mockCurrentProcessingDistribution(DATE_12, PACKING_WALL, ProcessPath.GLOBAL, 15),
            mockCurrentProcessingDistribution(DATE_13, PACKING_WALL, ProcessPath.GLOBAL, 15)
    );

    final List<CurrentProcessingDistribution> currentHeadcountProductivities = List.of(
            mockCurrentHeadcountProductivity(DATE_12, PICKING, 30),
            mockCurrentHeadcountProductivity(DATE_13, PICKING, 30),
            mockCurrentHeadcountProductivity(DATE_12, PACKING, 35),
            mockCurrentHeadcountProductivity(DATE_13, PACKING, 35),
            mockCurrentHeadcountProductivity(DATE_12, PACKING_WALL, 15),
            mockCurrentHeadcountProductivity(DATE_13, PACKING_WALL, 15)
    );

    final Workflow workflow = input.getWorkflow();

    when(processPathHeadcountShareService.getHeadcountShareByProcessPath(anyString(), any(), anySet(), any(), any(), any()))
            .thenReturn(processPathHeadcountResponse);

    final var allProcessingDistribution = new ArrayList<>(currentProcessingDistributions);
    allProcessingDistribution.addAll(currentHeadcountProductivities);

    when(currentProcessingRepository.saveAll(anyList()))
            .thenReturn(allProcessingDistribution);

    //WHEN
    final List<SimulationOutput> simulations = activateSimulationUseCase.execute(input);

    //THEN
    currentProcessingDistributions.stream()
            .map(CurrentProcessingDistribution::getProcessName)
            .distinct()
            .forEach(processName -> verify(currentProcessingRepository).deactivateProcessingDistribution(input.getWarehouseId(),
                    workflow,
                    processName,
                    List.of(DATE_12, DATE_13),
                    EFFECTIVE_WORKERS,
                    USER_ID,
                    WORKERS
            ));

    currentHeadcountProductivities.stream()
            .map(CurrentProcessingDistribution::getProcessName)
            .distinct()
            .forEach(processName -> verify(currentProcessingRepository).deactivateProcessingDistribution(input.getWarehouseId(),
                    workflow,
                    processName,
                    List.of(DATE_12, DATE_13),
                    ProcessingType.PRODUCTIVITY,
                    USER_ID,
                    UNITS_PER_HOUR
            ));

    final int expectedResultSize = currentHeadcountProductivities.size() + currentProcessingDistributions.size();
    assertEquals(expectedResultSize, simulations.size());

  }

  private SimulationInput mockSimulationInput(final List<Simulation> simulations) {
    return SimulationInput.builder()
        .warehouseId(WAREHOUSE_ID)
        .workflow(FBM_WMS_OUTBOUND)
        .simulations(simulations)
        .userId(USER_ID)
        .build();
  }

  private List<CurrentProcessingDistribution> mockCurrentDistribution() {
    return List.of(
        CurrentProcessingDistribution.builder()
            .date(DATE_12)
            .processName(PICKING)
            .processPath(ProcessPath.GLOBAL)
            .quantity(30)
            .quantityMetricUnit(WORKERS)
            .workflow(FBM_WMS_OUTBOUND)
            .type(EFFECTIVE_WORKERS)
            .isActive(true)
            .build(),
        CurrentProcessingDistribution.builder()
            .date(DATE_13)
            .processName(PICKING)
            .processPath(ProcessPath.GLOBAL)
            .quantity(25)
            .quantityMetricUnit(WORKERS)
            .workflow(FBM_WMS_OUTBOUND)
            .type(EFFECTIVE_WORKERS)
            .isActive(true)
            .build(),
        CurrentProcessingDistribution.builder()
            .date(DATE_12)
            .processName(PACKING)
            .processPath(ProcessPath.GLOBAL)
            .quantity(96)
            .quantityMetricUnit(UNITS_PER_HOUR)
            .type(ProcessingType.PRODUCTIVITY)
            .workflow(FBM_WMS_OUTBOUND)
            .isActive(true)
            .build()
    );
  }

  private CurrentProcessingDistribution mockCurrentProcessingDistribution(final ZonedDateTime date,
                                                                          final ProcessName processName,
                                                                          final ProcessPath processPath,
                                                                          final int quantity) {

    double ratio = mockProcessPathHeadcountShare()
            .getOrDefault(processPath, Map.of(processName, Map.of(date.toInstant(), 1.0D)))
            .getOrDefault(processName, Map.of(date.toInstant(), 1.0D))
            .getOrDefault(date.toInstant(), 1.0D);

    return CurrentProcessingDistribution.builder()
            .date(date)
            .processName(processName)
            .processPath(processPath)
            .quantity(quantity * ratio)
            .quantityMetricUnit(WORKERS)
            .workflow(FBM_WMS_OUTBOUND)
            .type(EFFECTIVE_WORKERS)
            .isActive(true)
            .build();
  }

  private CurrentProcessingDistribution mockCurrentHeadcountProductivity(final ZonedDateTime date,
                                                                        final ProcessName processName,
                                                                        final int productivity) {
    return CurrentProcessingDistribution.builder()
            .date(date)
            .processName(processName)
            .processPath(ProcessPath.GLOBAL)
            .quantity(productivity)
            .quantityMetricUnit(UNITS_PER_HOUR)
            .type(ProcessingType.PRODUCTIVITY)
            .workflow(FBM_WMS_OUTBOUND)
            .isActive(true)
            .build();
  }

  private Simulation mockSimulation(final ProcessName processName,
                                    final EntityType entityType,
                                    final int quantity) {
      return new Simulation(
              processName,
              singletonList(
                      new SimulationEntity(
                              entityType,
                              List.of(
                                      new QuantityByDate(DATE_12, quantity),
                                      new QuantityByDate(DATE_13, quantity)
                              )
                      )
              )
      );
  }

  private Map<ProcessPath, Map<ProcessName, Map<Instant, Double>>> mockProcessPathHeadcountShare() {
      return Map.of(
              ProcessPath.GLOBAL,
              Map.of(
                      PICKING,
                      Map.of(
                              DATE_12.toInstant(), 1.0D,
                              DATE_13.toInstant(), 1.0D
                      )
              ),
              ProcessPath.TOT_MULTI_ORDER,
              Map.of(
                      PICKING,
                      Map.of(
                              DATE_12.toInstant(), 0.7D,
                              DATE_13.toInstant(), 0.7D
                      )
              ),
              ProcessPath.NON_TOT_MULTI_ORDER,
              Map.of(
                      PICKING,
                      Map.of(
                              DATE_12.toInstant(), 0.3D,
                              DATE_13.toInstant(), 0.3D
                      )
              )
      );
  }
}
