package com.mercadolibre.planning.model.api.usecase;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.RECEIVING;
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

import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GetThroughputUseCaseTestDto {

  @Mock
  private GetProductivityEntityUseCase getProductivityEntityUseCase;

  @Mock
  private GetHeadcountEntityUseCase getHeadcountEntityUseCase;

  @InjectMocks
  private GetThroughputUseCase getForecastedThroughputUseCase;

  @Test
  @DisplayName("Get throughput entity when source is forecast")
  public void testGetThroughputOk() {
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
            .processingType(Set.of(ProcessingType.ACTIVE_WORKERS))
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
  public void testGetThroughputSimulationOk() {
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
}
