package com.mercadolibre.planning.model.api.usecase.entities.throughput.get;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.RECEIVING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.ACTIVE_WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockGetHeadcountEntityInput;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockGetThroughputEntityInput;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockHeadcountEntityOutput;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockHeadcountEntityOutputWhitSimulations;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockMultiFunctionalProductivityEntityOutput;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockProductivityEntityOutput;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockProductivityEntityOutputWithSimulations;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.FORECAST;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.SIMULATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.ProductivityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.util.TestUtils;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetThroughputUseCaseTest {

  @Mock
  private GetProductivityEntityUseCase getProductivityEntityUseCase;

  @Mock
  private GetHeadcountEntityUseCase getHeadcountEntityUseCase;

  @InjectMocks
  private GetThroughputUseCase getForecastedThroughputUseCase;

  public static List<ProductivityOutput> productivityFromDifferentProcessPaths() {
    return List.of(
        ProductivityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC)
            .processPath(TOT_MONO)
            .processName(PICKING)
            .metricUnit(UNITS_PER_HOUR)
            .source(FORECAST)
            .quantity(15)
            .abilityLevel(1)
            .build(),
        ProductivityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC)
            .processPath(TOT_MONO)
            .processName(PICKING)
            .metricUnit(UNITS_PER_HOUR)
            .source(FORECAST)
            .quantity(7.5)
            .abilityLevel(2)
            .build(),
        ProductivityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC.plusHours(1))
            .processPath(TOT_MONO)
            .processName(PICKING)
            .metricUnit(UNITS_PER_HOUR)
            .source(FORECAST)
            .quantity(10)
            .abilityLevel(1)
            .build(),
        ProductivityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC.plusHours(1))
            .processPath(TOT_MONO)
            .processName(PICKING)
            .metricUnit(UNITS_PER_HOUR)
            .source(FORECAST)
            .quantity(5)
            .abilityLevel(2)
            .build(),
        ProductivityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC.plusHours(2))
            .processPath(TOT_MONO)
            .processName(PICKING)
            .metricUnit(UNITS_PER_HOUR)
            .source(FORECAST)
            .quantity(11)
            .abilityLevel(1)
            .build(),
        ProductivityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC.plusHours(2))
            .processPath(TOT_MONO)
            .processName(PICKING)
            .metricUnit(UNITS_PER_HOUR)
            .source(FORECAST)
            .quantity(5.5)
            .abilityLevel(2)
            .build(),

        ProductivityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC)
            .processPath(NON_TOT_MONO)
            .processName(PICKING)
            .metricUnit(UNITS_PER_HOUR)
            .source(FORECAST)
            .quantity(9)
            .abilityLevel(1)
            .build(),
        ProductivityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC)
            .processPath(NON_TOT_MONO)
            .processName(PICKING)
            .metricUnit(UNITS_PER_HOUR)
            .source(FORECAST)
            .quantity(4.5)
            .abilityLevel(2)
            .build(),
        ProductivityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC.plusHours(1))
            .processPath(NON_TOT_MONO)
            .processName(PICKING)
            .metricUnit(UNITS_PER_HOUR)
            .source(FORECAST)
            .quantity(6)
            .abilityLevel(1)
            .build(),
        ProductivityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC.plusHours(1))
            .processPath(NON_TOT_MONO)
            .processName(PICKING)
            .metricUnit(UNITS_PER_HOUR)
            .source(FORECAST)
            .quantity(3)
            .abilityLevel(2)
            .build(),
        ProductivityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC.plusHours(2))
            .processPath(NON_TOT_MONO)
            .processName(PICKING)
            .metricUnit(UNITS_PER_HOUR)
            .source(FORECAST)
            .quantity(10)
            .abilityLevel(1)
            .build(),
        ProductivityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC.plusHours(2))
            .processPath(NON_TOT_MONO)
            .processName(PICKING)
            .metricUnit(UNITS_PER_HOUR)
            .source(FORECAST)
            .quantity(5)
            .abilityLevel(2)
            .build()
    );
  }

  @Test
  @DisplayName("Get throughput entity when source is forecast")
  void testGetThroughputOk() {
    // GIVEN
    final GetEntityInput input = mockGetThroughputEntityInput(FORECAST, null);
    when(getHeadcountEntityUseCase.execute(
        GetHeadcountInput.builder()
            .warehouseId(input.getWarehouseId()).entityType(HEADCOUNT)
            .workflow(input.getWorkflow())
            .source(FORECAST)
            .dateFrom(input.getDateFrom())
            .dateTo(input.getDateTo())
            .processName(input.getProcessName())
            .processingType(Set.of(ACTIVE_WORKERS))
            .build()))
        .thenReturn(mockHeadcountEntityOutput());

    when(getHeadcountEntityUseCase.execute(
        GetHeadcountInput.builder()
            .warehouseId(input.getWarehouseId()).entityType(THROUGHPUT)
            .workflow(input.getWorkflow())
            .source(FORECAST)
            .dateFrom(input.getDateFrom())
            .dateTo(input.getDateTo())
            .processName(List.of(RECEIVING))
            .build()))
        .thenReturn(new ArrayList<>());

    when(getProductivityEntityUseCase.execute(any()))
        .thenReturn(mockProductivityEntityOutput());

    // WHEN
    final List<EntityOutput> output = getForecastedThroughputUseCase.execute(input);

    // THEN
    assertEquals(4, output.size());
    final EntityOutput output1 = output.get(0);
    assertEquals(A_DATE_UTC.withFixedOffsetZone(), output1.getDate());
    assertEquals(PICKING, output1.getProcessName());
    assertEquals(4000, output1.getValue());
    assertEquals(UNITS_PER_HOUR, output1.getMetricUnit());
    assertEquals(FORECAST, output1.getSource());
    assertEquals(FBM_WMS_OUTBOUND, output1.getWorkflow());

    final EntityOutput output2 = output.get(1);
    assertEquals(A_DATE_UTC.plusHours(1).withFixedOffsetZone(), output2.getDate());
    assertEquals(PICKING, output2.getProcessName());
    assertEquals(2800, output2.getValue());
    assertEquals(UNITS_PER_HOUR, output2.getMetricUnit());
    assertEquals(FORECAST, output2.getSource());
    assertEquals(FBM_WMS_OUTBOUND, output2.getWorkflow());

    final EntityOutput output3 = output.get(2);
    assertEquals(A_DATE_UTC.withFixedOffsetZone(), output3.getDate());
    assertEquals(PACKING, output3.getProcessName());
    assertEquals(5100, output3.getValue());
    assertEquals(UNITS_PER_HOUR, output3.getMetricUnit());
    assertEquals(FORECAST, output3.getSource());
    assertEquals(FBM_WMS_OUTBOUND, output3.getWorkflow());

    final EntityOutput output4 = output.get(3);
    assertEquals(A_DATE_UTC.plusHours(1).withFixedOffsetZone(), output4.getDate());
    assertEquals(PACKING, output4.getProcessName());
    assertEquals(2700, output4.getValue());
    assertEquals(UNITS_PER_HOUR, output4.getMetricUnit());
    assertEquals(FORECAST, output4.getSource());
    assertEquals(FBM_WMS_OUTBOUND, output4.getWorkflow());
  }

  @Test
  @DisplayName("Get throughput entity when source is simulation")
  void testGetThroughputSimulationOk() {
    // GIVEN
    final GetEntityInput input = mockGetHeadcountEntityInput(SIMULATION);
    when(getHeadcountEntityUseCase.execute(any()))
        .thenReturn(mockHeadcountEntityOutputWhitSimulations());

    when(getProductivityEntityUseCase.execute(GetProductivityInput.builder()
        .warehouseId(input.getWarehouseId())
        .workflow(input.getWorkflow())
        .entityType(PRODUCTIVITY)
        .dateFrom(input.getDateFrom())
        .dateTo(input.getDateTo())
        .source(SIMULATION)
        .processPaths(List.of(ProcessPath.GLOBAL))
        .processName(input.getProcessName())
        .simulations(input.getSimulations())
        .abilityLevel(Set.of(1, 2))
        .viewDate(input.getViewDate())
        .build())
    ).thenReturn(
        Stream.concat(
                mockProductivityEntityOutputWithSimulations().stream(),
                mockMultiFunctionalProductivityEntityOutput().stream()
            )
            .collect(Collectors.toList())
    );

    // WHEN
    final List<EntityOutput> output = getForecastedThroughputUseCase.execute(input);

    // THEN
    // TODO Reviewers of the PR: please check this behaviour change is ok.
    assertEquals(3, output.size());
    final EntityOutput output1 = output.get(0);
    assertEquals(A_DATE_UTC.withFixedOffsetZone(), output1.getDate());
    assertEquals(PICKING, output1.getProcessName());
    assertEquals(4950, output1.getValue());
    assertEquals(UNITS_PER_HOUR, output1.getMetricUnit());
    assertEquals(SIMULATION, output1.getSource());
    assertEquals(FBM_WMS_OUTBOUND, output1.getWorkflow());

    final EntityOutput output2 = output.get(1);
    assertEquals(A_DATE_UTC.plusHours(1).withFixedOffsetZone(), output2.getDate());
    assertEquals(PICKING, output2.getProcessName());
    assertEquals(1500, output2.getValue());
    assertEquals(UNITS_PER_HOUR, output2.getMetricUnit());
    assertEquals(SIMULATION, output2.getSource());
    assertEquals(FBM_WMS_OUTBOUND, output2.getWorkflow());

    final EntityOutput output3 = output.get(2);
    assertEquals(A_DATE_UTC.withFixedOffsetZone(), output3.getDate());
    assertEquals(PACKING, output3.getProcessName());
    assertEquals(4853, output3.getValue());
    assertEquals(UNITS_PER_HOUR, output3.getMetricUnit());
    assertEquals(SIMULATION, output3.getSource());
    assertEquals(FBM_WMS_OUTBOUND, output3.getWorkflow());
  }

  @Test
  @DisplayName("when there are several process paths return simulations for each one")
  void testGetThroughputSimulationWithSeveralProcessPaths() {
    // GIVEN
    final GetEntityInput input = getEntityInput(List.of(TOT_MONO, NON_TOT_MONO));

    when(getHeadcountEntityUseCase.execute(any())).thenReturn(headcountFromDifferentProcessPaths());

    when(getProductivityEntityUseCase.execute(GetProductivityInput.builder()
        .warehouseId(input.getWarehouseId())
        .workflow(input.getWorkflow())
        .entityType(PRODUCTIVITY)
        .dateFrom(input.getDateFrom())
        .dateTo(input.getDateTo())
        .source(SIMULATION)
        .processPaths(List.of(TOT_MONO, NON_TOT_MONO))
        .processName(input.getProcessName())
        .simulations(input.getSimulations())
        .abilityLevel(Set.of(1, 2))
        .viewDate(input.getViewDate())
        .build())
    ).thenReturn(productivityFromDifferentProcessPaths());

    // WHEN
    final List<EntityOutput> output = getForecastedThroughputUseCase.execute(input);

    // THEN
    assertEquals(6, output.size());

    // TOT_MONO
    // 30 reps x 15 prod
    assertEntityOutput(output.get(0), FBM_WMS_OUTBOUND, TOT_MONO, PICKING, A_DATE_UTC, 450D);

    // 30.5 reps x 10 prod + 2.5 reps x 5 prod (10 x 0.5)
    assertEntityOutput(output.get(1), FBM_WMS_OUTBOUND, TOT_MONO, PICKING, A_DATE_UTC.plusHours(1), 317.5D);

    // 35 reps x 11 prod with ratio 1.0
    assertEntityOutput(output.get(2), FBM_WMS_OUTBOUND, TOT_MONO, PICKING, A_DATE_UTC.plusHours(2), 385D);

    // NON_TOT_MONO
    // 30 reps x 9 prod with ratio 1.0
    assertEntityOutput(output.get(3), FBM_WMS_OUTBOUND, NON_TOT_MONO, PICKING, A_DATE_UTC, 270D);

    // 45.5 reps x 6 prod - 2.5 reps x 6 prod
    assertEntityOutput(output.get(4), FBM_WMS_OUTBOUND, NON_TOT_MONO, PICKING, A_DATE_UTC.plusHours(1), 258D);

    // 35 reps x 10 prod
    assertEntityOutput(output.get(5), FBM_WMS_OUTBOUND, NON_TOT_MONO, PICKING, A_DATE_UTC.plusHours(2), 350D);
  }

  private void assertEntityOutput(
      final EntityOutput entity,
      final Workflow workflow,
      final ProcessPath processPath,
      final ProcessName process,
      final ZonedDateTime date,
      final Double quantity
  ) {
    assertEquals(workflow, entity.getWorkflow());
    assertEquals(processPath, entity.getProcessPath());
    assertEquals(process, entity.getProcessName());
    assertEquals(date.withFixedOffsetZone(), entity.getDate().withFixedOffsetZone());
    assertEquals(quantity, entity.getQuantity());
  }

  private GetEntityInput getEntityInput(final List<ProcessPath> processPaths) {
    return GetEntityInput.builder()
        .warehouseId(TestUtils.WAREHOUSE_ID)
        .workflow(FBM_WMS_OUTBOUND)
        .entityType(THROUGHPUT)
        .dateFrom(A_DATE_UTC)
        .dateTo(A_DATE_UTC.plusDays(2))
        .source(SIMULATION)
        .processPaths(processPaths)
        .processName(List.of(PICKING))
        .viewDate(A_DATE_UTC.toInstant())
        .build();
  }

  private List<EntityOutput> headcountFromDifferentProcessPaths() {
    return List.of(
        EntityOutput.builder()
            .date(A_DATE_UTC)
            .processPath(TOT_MONO)
            .processName(PICKING)
            .quantity(30)
            .metricUnit(WORKERS)
            .type(ACTIVE_WORKERS)
            .source(FORECAST)
            .build(),
        EntityOutput.builder()
            .date(A_DATE_UTC.plusHours(1))
            .processPath(TOT_MONO)
            .processName(PICKING)
            .quantity(30.5)
            .metricUnit(WORKERS)
            .type(ACTIVE_WORKERS)
            .source(FORECAST)
            .build(),
        EntityOutput.builder()
            .date(A_DATE_UTC.plusHours(1))
            .processPath(TOT_MONO)
            .processName(PICKING)
            .quantity(33)
            .metricUnit(WORKERS)
            .type(ACTIVE_WORKERS)
            .source(SIMULATION)
            .build(),
        EntityOutput.builder()
            .date(A_DATE_UTC.plusHours(2))
            .processPath(TOT_MONO)
            .processName(PICKING)
            .quantity(35)
            .metricUnit(WORKERS)
            .type(ACTIVE_WORKERS)
            .source(FORECAST)
            .build(),
        EntityOutput.builder()
            .date(A_DATE_UTC)
            .processPath(NON_TOT_MONO)
            .processName(PICKING)
            .quantity(30)
            .metricUnit(WORKERS)
            .type(ACTIVE_WORKERS)
            .source(FORECAST)
            .build(),
        EntityOutput.builder()
            .date(A_DATE_UTC.plusHours(1))
            .processPath(NON_TOT_MONO)
            .processName(PICKING)
            .quantity(45.5)
            .metricUnit(WORKERS)
            .type(ACTIVE_WORKERS)
            .source(FORECAST)
            .build(),
        EntityOutput.builder()
            .date(A_DATE_UTC.plusHours(1))
            .processPath(NON_TOT_MONO)
            .processName(PICKING)
            .quantity(43)
            .metricUnit(WORKERS)
            .type(ACTIVE_WORKERS)
            .source(SIMULATION)
            .build(),
        EntityOutput.builder()
            .date(A_DATE_UTC.plusHours(2))
            .processPath(NON_TOT_MONO)
            .processName(PICKING)
            .quantity(35)
            .metricUnit(WORKERS)
            .type(ACTIVE_WORKERS)
            .source(FORECAST)
            .build()
    );
  }
}
