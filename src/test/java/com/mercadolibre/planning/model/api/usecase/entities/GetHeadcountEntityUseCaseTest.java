package com.mercadolibre.planning.model.api.usecase.entities;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.GLOBAL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.ACTIVE_WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.EFFECTIVE_WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockCurrentProcDist;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockGetHeadcountEntityInput;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.MAX_CAPACITY;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.FORECAST;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.SIMULATION;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.usecase.ProcessingDistributionViewImpl;
import com.mercadolibre.planning.model.api.web.controller.projection.request.QuantityByDate;
import com.mercadolibre.planning.model.api.web.controller.projection.request.Source;
import com.mercadolibre.planning.model.api.web.controller.simulation.Simulation;
import com.mercadolibre.planning.model.api.web.controller.simulation.SimulationEntity;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetHeadcountEntityUseCaseTest {

  @Mock
  private ProcessingDistributionRepository processingDistRepository;

  @Mock
  private CurrentProcessingDistributionRepository currentRepository;

  @Mock
  private GetForecastUseCase getForecastUseCase;

  @InjectMocks
  private GetHeadcountEntityUseCase getHeadcountEntityUseCase;

  @Test
  @DisplayName("Get headcount entity when source is forecast")
  void testGetHeadcountOk() {
    // GIVEN
    final GetHeadcountInput input = mockGetHeadcountEntityInput(FORECAST, Set.of(EFFECTIVE_WORKERS, ACTIVE_WORKERS), null);
    final List<Long> forecastIds = List.of(1L);

    when(getForecastUseCase.execute(GetForecastInput.builder()
        .workflow(input.getWorkflow())
        .warehouseId(input.getWarehouseId())
        .dateFrom(input.getDateFrom())
        .dateTo(input.getDateTo())
        .viewDate(A_DATE_UTC.toInstant())
        .build())
    ).thenReturn(forecastIds);

    when(processingDistRepository.findByTypeProcessPathProcessNameAndDateInRange(
        Set.of(EFFECTIVE_WORKERS.name(), ACTIVE_WORKERS.name()),
        List.of(ProcessPath.GLOBAL.toString()),
        List.of(PICKING.name(), PACKING.name()),
        A_DATE_UTC, A_DATE_UTC.plusDays(2),
        forecastIds)
    ).thenReturn(processingDistributions());

    // WHEN
    final List<EntityOutput> output = getHeadcountEntityUseCase.execute(input);

    // THEN
    verifyNoInteractions(currentRepository);
    final EntityOutput output1 = output.get(0);
    assertEquals(A_DATE_UTC.toInstant(), output1.getDate().toInstant());
    assertEquals(PICKING, output1.getProcessName());
    assertEquals(100, output1.getValue());
    assertEquals(MetricUnit.WORKERS, output1.getMetricUnit());
    assertEquals(FORECAST, output1.getSource());
    assertEquals(FBM_WMS_OUTBOUND, output1.getWorkflow());

    final EntityOutput output2 = output.get(1);
    assertEquals(A_DATE_UTC.plusHours(1).toInstant(), output2.getDate().toInstant());
    assertEquals(PICKING, output2.getProcessName());
    assertEquals(120, output2.getValue());
    assertEquals(MetricUnit.WORKERS, output2.getMetricUnit());
    assertEquals(FORECAST, output2.getSource());
    assertEquals(FBM_WMS_OUTBOUND, output2.getWorkflow());
  }

  @Test
  @DisplayName("Get headcount entity when source is null and has simulations applied")
  void testGetHeadcountWithUnsavedSimulationOk() {
    // GIVEN
    final GetHeadcountInput input = mockGetHeadcountEntityInput(null,
        Set.of(EFFECTIVE_WORKERS, ACTIVE_WORKERS),
        List.of(new Simulation(
                PICKING,
                List.of(new SimulationEntity(
                    HEADCOUNT,
                    List.of(new QuantityByDate(A_DATE_UTC, 50D, null))))),
            new Simulation(
                PACKING,
                List.of(new SimulationEntity(
                    HEADCOUNT,
                    List.of(new QuantityByDate(A_DATE_UTC, 100D, null))))),
            new Simulation(
                GLOBAL,
                List.of(new SimulationEntity(
                    MAX_CAPACITY,
                    List.of(new QuantityByDate(A_DATE_UTC, 100D, null)))))));

    final List<Long> forecastIds = List.of(1L);

    when(getForecastUseCase.execute(GetForecastInput.builder()
        .workflow(input.getWorkflow())
        .warehouseId(input.getWarehouseId())
        .dateFrom(input.getDateFrom())
        .dateTo(input.getDateTo())
        .viewDate(A_DATE_UTC.toInstant())
        .build())
    ).thenReturn(forecastIds);

    when(currentRepository.findSimulationByWarehouseIdWorkflowTypeProcessNameAndDateInRangeAtViewDate(
        WAREHOUSE_ID,
        FBM_WMS_OUTBOUND.name(),
        Set.of(ProcessPath.GLOBAL.name()),
        Set.of(PICKING.name(), PACKING.name()),
        Set.of(EFFECTIVE_WORKERS.name(), ACTIVE_WORKERS.name()),
        input.getDateFrom(),
        input.getDateTo(),
        A_DATE_UTC.toInstant()
    )).thenReturn(List.of(
        mockCurrentProcDist(A_DATE_UTC.withFixedOffsetZone(), 40L),
        mockCurrentProcDist(A_DATE_UTC.plusHours(1).withFixedOffsetZone(), 60L)
    ));

    when(processingDistRepository.findByTypeProcessPathProcessNameAndDateInRange(
        Set.of(EFFECTIVE_WORKERS.name(), ACTIVE_WORKERS.name()),
        List.of(ProcessPath.GLOBAL.toString()),
        List.of(PICKING.name(), PACKING.name()),
        A_DATE_UTC, A_DATE_UTC.plusDays(2),
        forecastIds)
    ).thenReturn(processingDistributions());

    // WHEN
    final List<EntityOutput> output = getHeadcountEntityUseCase.execute(input);

    // THEN
    assertEquals(7, output.size());
    final EntityOutput output1 = output.get(0);
    assertEquals(A_DATE_UTC.toInstant(), output1.getDate().toInstant());
    assertEquals(PICKING, output1.getProcessName());
    assertEquals(100, output1.getValue());
    assertEquals(MetricUnit.WORKERS, output1.getMetricUnit());
    assertEquals(FORECAST, output1.getSource());
    assertEquals(FBM_WMS_OUTBOUND, output1.getWorkflow());

    final EntityOutput output2 = output.get(1);
    assertEquals(A_DATE_UTC.plusHours(1).toInstant(), output2.getDate().toInstant());
    assertEquals(PICKING, output2.getProcessName());
    assertEquals(120, output2.getValue());
    assertEquals(MetricUnit.WORKERS, output2.getMetricUnit());
    assertEquals(FORECAST, output2.getSource());
    assertEquals(FBM_WMS_OUTBOUND, output2.getWorkflow());

    final EntityOutput output3 = output.get(2);
    assertEquals(A_DATE_UTC.plusHours(2).toInstant(), output3.getDate().toInstant());
    assertEquals(PACKING, output3.getProcessName());
    assertEquals(120, output3.getValue());
    assertEquals(MetricUnit.WORKERS, output3.getMetricUnit());
    assertEquals(FORECAST, output3.getSource());
    assertEquals(FBM_WMS_OUTBOUND, output3.getWorkflow());

    final EntityOutput output4 = output.get(3);
    assertEquals(A_DATE_UTC.toInstant(), output4.getDate().toInstant());
    assertEquals(PICKING, output4.getProcessName());
    assertEquals(50, output4.getValue());
    assertEquals(MetricUnit.WORKERS, output4.getMetricUnit());
    assertEquals(SIMULATION, output4.getSource());
    assertEquals(FBM_WMS_OUTBOUND, output4.getWorkflow());

    final EntityOutput output5 = output.get(4);
    assertEquals(A_DATE_UTC.toInstant(), output5.getDate().toInstant());
    assertEquals(PACKING, output5.getProcessName());
    assertEquals(100, output5.getValue());
    assertEquals(MetricUnit.WORKERS, output5.getMetricUnit());
    assertEquals(SIMULATION, output5.getSource());
    assertEquals(FBM_WMS_OUTBOUND, output5.getWorkflow());

    final EntityOutput output6 = output.get(5);
    assertEquals(GLOBAL, output6.getProcessName());
    assertEquals(UNITS_PER_HOUR, output6.getMetricUnit());
    assertEquals(SIMULATION, output6.getSource());
    assertEquals(FBM_WMS_OUTBOUND, output6.getWorkflow());

    final EntityOutput output7 = output.get(6);
    assertEquals(A_DATE_UTC.plusHours(1).toInstant(), output7.getDate().toInstant());
    assertEquals(PACKING, output7.getProcessName());
    assertEquals(60, output7.getValue());
    assertEquals(MetricUnit.WORKERS, output7.getMetricUnit());
    assertEquals(SIMULATION, output7.getSource());
    assertEquals(FBM_WMS_OUTBOUND, output7.getWorkflow());
  }

  @Test
  @DisplayName("Get headcount entity when source is simulation")
  void testGetHeadcountFromSourceSimulation() {
    // GIVEN
    final GetHeadcountInput input = mockGetHeadcountEntityInput(SIMULATION);
    final List<Long> forecastIds = singletonList(1L);

    when(getForecastUseCase.execute(GetForecastInput.builder()
        .workflow(input.getWorkflow())
        .warehouseId(input.getWarehouseId())
        .dateFrom(input.getDateFrom())
        .dateTo(input.getDateTo())
        .viewDate(A_DATE_UTC.toInstant())
        .build())
    ).thenReturn(forecastIds);

    when(processingDistRepository.findByTypeProcessPathProcessNameAndDateInRange(
        null,
        List.of(ProcessPath.GLOBAL.toString()),
        input.getProcessNamesAsString(),
        input.getDateFrom(),
        input.getDateTo(),
        forecastIds)
    ).thenReturn(processingDistributions());

    when(currentRepository.findSimulationByWarehouseIdWorkflowTypeProcessNameAndDateInRangeAtViewDate(
            input.getWarehouseId(),
            FBM_WMS_OUTBOUND.name(),
            Set.of(ProcessPath.GLOBAL.name()),
            Set.of(PICKING.name(), PACKING.name()),
            Set.of(EFFECTIVE_WORKERS.name()),
            input.getDateFrom(),
            input.getDateTo(),
            A_DATE_UTC.toInstant()
        )
    ).thenReturn(currentDistribution());

    // WHEN
    final List<EntityOutput> output = getHeadcountEntityUseCase.execute(input);

    // THEN
    assertEquals(5, output.size());
    outputPropertiesEqualTo(output.get(0), ProcessPath.GLOBAL, FORECAST, 100, A_DATE_UTC.toInstant());
    outputPropertiesEqualTo(output.get(1), ProcessPath.GLOBAL, FORECAST, 120, A_DATE_UTC.plusHours(1).toInstant());
    outputPropertiesEqualTo(output.get(2), ProcessPath.GLOBAL, FORECAST, 120, A_DATE_UTC.plusHours(2).toInstant());
    outputPropertiesEqualTo(output.get(3), ProcessPath.GLOBAL, SIMULATION, 20, A_DATE_UTC.toInstant());
    outputPropertiesEqualTo(output.get(4), ProcessPath.GLOBAL, SIMULATION, 20, A_DATE_UTC.plusHours(1).toInstant());
  }

  @ParameterizedTest
  @MethodSource("testProcessPaths")
  @DisplayName("Get headcount entity when source is simulation and there area multiple process paths")
  void testProcessPaths(List<ProcessPath> processPaths) {
    // GIVEN
    final GetHeadcountInput input = mockGetHeadcountEntityInput(
        processPaths,
        SIMULATION,
        Set.of(EFFECTIVE_WORKERS),
        List.of()
    );
    final List<Long> forecastIds = singletonList(1L);

    when(getForecastUseCase.execute(GetForecastInput.builder()
        .workflow(input.getWorkflow())
        .warehouseId(input.getWarehouseId())
        .dateFrom(input.getDateFrom())
        .dateTo(input.getDateTo())
        .viewDate(A_DATE_UTC.toInstant())
        .build())
    ).thenReturn(forecastIds);

    when(processingDistRepository.findByTypeProcessPathProcessNameAndDateInRange(
        Set.of(EFFECTIVE_WORKERS.name()),
        processPaths == null || processPaths.isEmpty()
            ? List.of()
            : processPaths.stream().map(ProcessPath::toString).toList(),
        input.getProcessNamesAsString(),
        input.getDateFrom(),
        input.getDateTo(),
        forecastIds)
    ).thenReturn(processingDistributionsFromProcessPaths());

    when(currentRepository.findSimulationByWarehouseIdWorkflowTypeProcessNameAndDateInRangeAtViewDate(
            input.getWarehouseId(),
            FBM_WMS_OUTBOUND.name(),
            processPaths == null || processPaths.isEmpty()
                ? Set.of()
                : processPaths.stream().map(ProcessPath::toString).collect(Collectors.toSet()),
        Set.of(PICKING.name(), PACKING.name()),
            Set.of(EFFECTIVE_WORKERS.name()),
            input.getDateFrom(),
            input.getDateTo(),
            A_DATE_UTC.toInstant()
        )
    ).thenReturn(currentDistributionByProcessPath());

    // WHEN
    final List<EntityOutput> output = getHeadcountEntityUseCase.execute(input);

    // THEN
    assertEquals(8, output.size());
    outputPropertiesEqualTo(output.get(0), TOT_MONO, FORECAST, 100, A_DATE_UTC.toInstant());
    outputPropertiesEqualTo(output.get(1), TOT_MONO, FORECAST, 125, A_DATE_UTC.plusHours(1).toInstant());
    outputPropertiesEqualTo(output.get(2), TOT_MONO, FORECAST, 150, A_DATE_UTC.plusHours(2).toInstant());
    outputPropertiesEqualTo(output.get(3), NON_TOT_MONO, FORECAST, 230, A_DATE_UTC.toInstant());
    outputPropertiesEqualTo(output.get(4), NON_TOT_MONO, FORECAST, 260, A_DATE_UTC.plusHours(1).toInstant());
    outputPropertiesEqualTo(output.get(5), NON_TOT_MONO, FORECAST, 280, A_DATE_UTC.plusHours(2).toInstant());
    outputPropertiesEqualTo(output.get(6), TOT_MONO, SIMULATION, 40, A_DATE_UTC.plusHours(1).toInstant());
    outputPropertiesEqualTo(output.get(7), NON_TOT_MONO, SIMULATION, 60, A_DATE_UTC.plusHours(1).toInstant());
  }

  private static Stream<Arguments> testProcessPaths() {
    return Stream.of(
        Arguments.of(List.of(TOT_MONO, NON_TOT_MONO)),
        Arguments.of(List.of()),
        null
    );
  }

  private List<ProcessingDistributionView> processingDistributions() {
    return List.of(
        new ProcessingDistributionViewImpl(
            1,
            Date.from(A_DATE_UTC.toInstant()),
            ProcessPath.GLOBAL,
            PICKING,
            100,
            MetricUnit.WORKERS,
            EFFECTIVE_WORKERS),
        new ProcessingDistributionViewImpl(
            2,
            Date.from(A_DATE_UTC.plusHours(1).toInstant()),
            ProcessPath.GLOBAL,
            PICKING,
            120,
            MetricUnit.WORKERS,
            EFFECTIVE_WORKERS),
        new ProcessingDistributionViewImpl(
            3,
            Date.from(A_DATE_UTC.plusHours(2).toInstant()),
            ProcessPath.GLOBAL,
            PACKING,
            120,
            MetricUnit.WORKERS,
            EFFECTIVE_WORKERS)
    );
  }

  private List<ProcessingDistributionView> processingDistributionsFromProcessPaths() {
    return List.of(
        new ProcessingDistributionViewImpl(
            1,
            Date.from(A_DATE_UTC.toInstant()),
            TOT_MONO,
            PICKING,
            100,
            MetricUnit.WORKERS,
            EFFECTIVE_WORKERS),
        new ProcessingDistributionViewImpl(
            2,
            Date.from(A_DATE_UTC.plusHours(1).toInstant()),
            TOT_MONO,
            PICKING,
            125,
            MetricUnit.WORKERS,
            EFFECTIVE_WORKERS),
        new ProcessingDistributionViewImpl(
            3,
            Date.from(A_DATE_UTC.plusHours(2).toInstant()),
            TOT_MONO,
            PICKING,
            150,
            MetricUnit.WORKERS,
            EFFECTIVE_WORKERS),
        new ProcessingDistributionViewImpl(
            1,
            Date.from(A_DATE_UTC.toInstant()),
            NON_TOT_MONO,
            PICKING,
            230,
            MetricUnit.WORKERS,
            EFFECTIVE_WORKERS),
        new ProcessingDistributionViewImpl(
            2,
            Date.from(A_DATE_UTC.plusHours(1).toInstant()),
            NON_TOT_MONO,
            PICKING,
            260,
            MetricUnit.WORKERS,
            EFFECTIVE_WORKERS),
        new ProcessingDistributionViewImpl(
            3,
            Date.from(A_DATE_UTC.plusHours(2).toInstant()),
            NON_TOT_MONO,
            PICKING,
            280,
            MetricUnit.WORKERS,
            EFFECTIVE_WORKERS)
    );
  }

  private List<CurrentProcessingDistribution> currentDistribution() {
    return List.of(
        CurrentProcessingDistribution.builder()
            .date(A_DATE_UTC)
            .type(EFFECTIVE_WORKERS)
            .quantity(20)
            .isActive(true)
            .quantityMetricUnit(MetricUnit.WORKERS)
            .processPath(ProcessPath.GLOBAL)
            .processName(PICKING)
            .build(),
        CurrentProcessingDistribution.builder()
            .date(A_DATE_UTC.plusHours(1))
            .type(EFFECTIVE_WORKERS)
            .quantity(20)
            .isActive(true)
            .quantityMetricUnit(MetricUnit.WORKERS)
            .processPath(ProcessPath.GLOBAL)
            .processName(PACKING)
            .build()
    );
  }

  private List<CurrentProcessingDistribution> currentDistributionByProcessPath() {
    return List.of(
        CurrentProcessingDistribution.builder()
            .date(A_DATE_UTC.plusHours(1))
            .type(EFFECTIVE_WORKERS)
            .quantity(40)
            .isActive(true)
            .quantityMetricUnit(MetricUnit.WORKERS)
            .processPath(TOT_MONO)
            .processName(PICKING)
            .build(),
        CurrentProcessingDistribution.builder()
            .date(A_DATE_UTC.plusHours(1))
            .type(EFFECTIVE_WORKERS)
            .quantity(60)
            .isActive(true)
            .quantityMetricUnit(MetricUnit.WORKERS)
            .processPath(NON_TOT_MONO)
            .processName(PACKING)
            .build()
    );
  }

  private void outputPropertiesEqualTo(
      final EntityOutput entityOutput,
      final ProcessPath processPath,
      final Source source,
      final int quantity,
      final Instant instant
  ) {
    assertEquals(processPath, entityOutput.getProcessPath());
    assertEquals(source, entityOutput.getSource());
    assertEquals(quantity, entityOutput.getValue());
    assertEquals(instant, entityOutput.getDate().toInstant());
  }
}
