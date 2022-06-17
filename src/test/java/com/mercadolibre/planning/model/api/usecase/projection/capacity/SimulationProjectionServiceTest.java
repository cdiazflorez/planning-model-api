package com.mercadolibre.planning.model.api.usecase.projection.capacity;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseInput;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseOutput;
import com.mercadolibre.planning.model.api.domain.entity.sla.ProcessingTime;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeInput;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeService;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.Backlog;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.DeliveryPromiseProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.SimulationProjectionService;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetDeliveryPromiseProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.sla.GetSlaByWarehouseOutboundService;
import com.mercadolibre.planning.model.api.usecase.ProcessingDistributionViewImpl;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import com.mercadolibre.planning.model.api.web.controller.projection.request.QuantityByDate;
import com.mercadolibre.planning.model.api.web.controller.simulation.Simulation;
import com.mercadolibre.planning.model.api.web.controller.simulation.SimulationEntity;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SimulationProjectionServiceTest {

  private static final ZonedDateTime NOW = ZonedDateTime.now(UTC);

  private static final ZonedDateTime CPT1 = NOW.plusHours(1);

  private static final ZonedDateTime CPT2 = NOW.plusHours(2);

  private static final Map<ZonedDateTime, Long> CYCLE_TIME_BY_CPT = Map.of(CPT1, 360L, CPT2, 360L);

  private static final List<Long> FORECAST_IDS = List.of(1L, 2L);

  private static final String WH = "ARBA01";

  @InjectMocks
  private SimulationProjectionService simulationProjectionService;

  @Mock
  private ProcessingDistributionRepository processingDistRepository;

  @Mock
  private GetForecastUseCase getForecastUseCase;

  @Mock
  private GetCycleTimeService getCycleTimeService;

  @Mock
  private GetSlaByWarehouseOutboundService getSlaByWarehouseOutboundService;

  @Test
  public void executeTest() {

    final ZonedDateTime dateFrom = NOW;
    final ZonedDateTime dateTo = NOW.plusHours(6);
    final String logisticCenterId = WH;
    final Workflow workflow = FBM_WMS_OUTBOUND;

    when(getSlaByWarehouseOutboundService.execute(any(GetSlaByWarehouseInput.class))).thenReturn(mockSlasByWarehouse());
    when(getCycleTimeService.execute(any(GetCycleTimeInput.class))).thenReturn(CYCLE_TIME_BY_CPT);
    when(getForecastUseCase.execute(any(GetForecastInput.class))).thenReturn(FORECAST_IDS);
    when(processingDistRepository.findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(any(), any(), any(), any(), any()))
        .thenReturn(mockProcessingDistributions());

    List<DeliveryPromiseProjectionOutput> result = simulationProjectionService.execute(GetDeliveryPromiseProjectionInput.builder()
        .warehouseId(logisticCenterId)
        .workflow(workflow)
        .dateFrom(dateFrom)
        .dateTo(dateTo)
        .backlog(List.of(new Backlog(CPT1, 10000), new Backlog(CPT2, 20000)))
        .simulations(List.of(
            new Simulation(ProcessName.GLOBAL,
                List.of(
                    new SimulationEntity(
                        EntityType.THROUGHPUT,
                        List.of(
                            new QuantityByDate(CPT1.truncatedTo(ChronoUnit.SECONDS), 4),
                            new QuantityByDate(CPT2.truncatedTo(ChronoUnit.SECONDS), 5)
                        )
                    )
                )
            )
        ))
        .build());

    final var processingTime = new ProcessingTime(360L, MINUTES);

    final List<DeliveryPromiseProjectionOutput> expected = List.of(
        new DeliveryPromiseProjectionOutput(CPT1, CPT1.minusHours(2), 0, CPT1.minusHours(6),
            processingTime, CPT1.minusHours(7), false),
        new DeliveryPromiseProjectionOutput(CPT2, CPT2.minusHours(2), 0, CPT2.minusHours(6),
            processingTime, CPT1.minusHours(7), false)
    );

    assertEquals(expected.size(), result.size());
    assertEquals(expected.get(0).getDate(), result.get(0).getDate());
    assertEquals(expected.get(0).getEtdCutoff(), result.get(0).getEtdCutoff());
    assertEquals(expected.get(0).isDeferred(), result.get(0).isDeferred());
    assertEquals(expected.get(0).getProcessingTime(), result.get(0).getProcessingTime());

  }


  private static List<GetSlaByWarehouseOutput> mockSlasByWarehouse() {
    return List.of(
        GetSlaByWarehouseOutput.builder().date(CPT1).processingTime(new ProcessingTime(360L, MINUTES)).build(),
        GetSlaByWarehouseOutput.builder().date(CPT2).processingTime(new ProcessingTime(360L, MINUTES)).build()
    );
  }

  private static List<ProcessingDistributionView> mockProcessingDistributions() {
    return List.of(
        ProcessingDistributionViewImpl.builder()
            .date(Date.from(NOW.toLocalDateTime().plusHours(1).toInstant(UTC).truncatedTo(ChronoUnit.SECONDS)))
            .quantity(120L).build(),
        ProcessingDistributionViewImpl.builder()
            .date(Date.from(NOW.toLocalDateTime().plusHours(2).toInstant(UTC).truncatedTo(ChronoUnit.SECONDS)))
            .quantity(100L).build(),
        ProcessingDistributionViewImpl.builder()
            .date(Date.from(NOW.toLocalDateTime().plusHours(3).toInstant(UTC).truncatedTo(ChronoUnit.SECONDS)))
            .quantity(130L).build(),
        ProcessingDistributionViewImpl.builder()
            .date(Date.from(NOW.toLocalDateTime().plusHours(5).toInstant(UTC).truncatedTo(ChronoUnit.SECONDS)))
            .quantity(100L).build()
    );
  }

}
