package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentHeadcountProductivityRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.HeadcountProductivityRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.HeadcountProductivityView;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentHeadcountProductivity;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.ProductivityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.web.controller.projection.request.QuantityByDate;
import com.mercadolibre.planning.model.api.web.controller.projection.request.Source;
import com.mercadolibre.planning.model.api.web.controller.simulation.Simulation;
import com.mercadolibre.planning.model.api.web.controller.simulation.SimulationEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockCurrentProdEntity;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockGetProductivityEntityInput;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.FORECAST;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.SIMULATION;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetProductivityEntityUseCaseTest {

    @Mock
    private HeadcountProductivityRepository productivityRepository;

    @Mock
    private CurrentHeadcountProductivityRepository currentProductivityRepository;

    @Mock
    private GetForecastUseCase getForecastUseCase;

    @InjectMocks
    private GetProductivityEntityUseCase getProductivityEntityUseCase;

    @Test
    @DisplayName("Get productivity entity when source is forecast")
    public void testGetProductivityOk() {
        // GIVEN
        final GetProductivityInput input = mockGetProductivityEntityInput(FORECAST, null);
        final List<Long> forecastIds = singletonList(1L);

        when(getForecastUseCase.execute(GetForecastInput.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getWarehouseId())
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .build())
        ).thenReturn(forecastIds);

        when(productivityRepository.findBy(
                List.of(PICKING.name(), PACKING.name()),
                input.getDateFrom(),
                input.getDateTo(),
                forecastIds,
                Set.of(1))
        ).thenReturn(productivities());

        // WHEN
        final List<ProductivityOutput> output = getProductivityEntityUseCase.execute(input);

        // THEN
        assertEquals(4, output.size());
        verifyZeroInteractions(currentProductivityRepository);
        outputPropertiesEqualTo(output.get(0), PICKING, FORECAST, 80);
        outputPropertiesEqualTo(output.get(1), PICKING, FORECAST, 85);
        outputPropertiesEqualTo(output.get(2), PACKING, FORECAST, 90);
        outputPropertiesEqualTo(output.get(3), PACKING, FORECAST, 92);
    }

    @Test
    @DisplayName("Get productivity entity when source is null and has simulations applied")
    public void testGetProductivityWithUnsavedSimulationOk() {
        // GIVEN
        final GetProductivityInput input = mockGetProductivityEntityInput(
                null,
                List.of(new Simulation(
                        PICKING,
                        List.of(new SimulationEntity(
                                PRODUCTIVITY,
                                List.of(new QuantityByDate(A_DATE_UTC, 100),
                                        new QuantityByDate(A_DATE_UTC.plusHours(1), 101)))))));

        final List<Long> forecastIds = List.of(1L);

        when(getForecastUseCase.execute(GetForecastInput.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getWarehouseId())
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .build())
        ).thenReturn(forecastIds);

        when(currentProductivityRepository
                .findSimulationByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                        WAREHOUSE_ID,
                        FBM_WMS_OUTBOUND,
                        List.of(PICKING, PACKING),
                        input.getDateFrom(),
                        input.getDateTo()
                )
        ).thenReturn(List.of(
                mockCurrentProdEntity(A_DATE_UTC, 68),
                mockCurrentProdEntity(A_DATE_UTC.plusHours(1), 30)
        ));

        when(productivityRepository.findBy(
                List.of(PICKING.name(), PACKING.name()),
                input.getDateFrom(),
                input.getDateTo(),
                forecastIds,
                Set.of(1))
        ).thenReturn(productivities());

        // WHEN
        final List<ProductivityOutput> output = getProductivityEntityUseCase.execute(input);

        // THEN
        assertEquals(6, output.size());

        outputPropertiesEqualTo(output.get(0), PICKING, FORECAST, 80);
        outputPropertiesEqualTo(output.get(1), PICKING, FORECAST, 85);
        outputPropertiesEqualTo(output.get(2), PACKING, FORECAST, 90);
        outputPropertiesEqualTo(output.get(3), PACKING, FORECAST, 92);
        outputPropertiesEqualTo(output.get(4), PICKING, SIMULATION, 100);
        outputPropertiesEqualTo(output.get(5), PICKING, SIMULATION, 101);
    }

    @Test
    @DisplayName("Get productivity entity when source is simulation")
    public void testGetProductivityFromSourceSimulation() {
        // GIVEN
        final GetProductivityInput input = mockGetProductivityEntityInput(SIMULATION, null);
        final CurrentHeadcountProductivity currentProd = mockCurrentProdEntity(A_DATE_UTC, 68L);
        final List<Long> forecastIds = List.of(1L);

        // WHEN
        when(getForecastUseCase.execute(GetForecastInput.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getWarehouseId())
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .build())
        ).thenReturn(forecastIds);

        when(productivityRepository.findBy(
                List.of(PICKING.name(), PACKING.name()),
                input.getDateFrom(),
                input.getDateTo(),
                forecastIds,
                Set.of(1))
        ).thenReturn(productivities());

        when(currentProductivityRepository
                .findSimulationByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                        currentProd.getLogisticCenterId(),
                        FBM_WMS_OUTBOUND,
                        List.of(PICKING, PACKING),
                        input.getDateFrom(),
                        input.getDateTo()
                )
        ).thenReturn(currentProductivities());

        final List<ProductivityOutput> output = getProductivityEntityUseCase.execute(input);

        // THEN
        assertThat(output).isNotEmpty();
        assertEquals(5, output.size());
        outputPropertiesEqualTo(output.get(0), PICKING, FORECAST, 80);
        outputPropertiesEqualTo(output.get(1), PICKING, FORECAST, 85);
        outputPropertiesEqualTo(output.get(2), PACKING, FORECAST, 90);
        outputPropertiesEqualTo(output.get(3), PACKING, FORECAST, 92);
        outputPropertiesEqualTo(output.get(4), PICKING, SIMULATION, 68);
    }

    private List<HeadcountProductivityView> productivities() {
        return List.of(
                new HeadcountProductivityViewImpl(PICKING,
                        80, UNITS_PER_HOUR, Date.from(A_DATE_UTC.toInstant()), 1),
                new HeadcountProductivityViewImpl(PICKING,
                        85, UNITS_PER_HOUR, Date.from(A_DATE_UTC.plusHours(1).toInstant()), 1),
                new HeadcountProductivityViewImpl(PACKING,
                        90, UNITS_PER_HOUR, Date.from(A_DATE_UTC.toInstant()), 1),
                new HeadcountProductivityViewImpl(PACKING,
                        92, UNITS_PER_HOUR, Date.from(A_DATE_UTC.plusHours(1).toInstant()),1)
        );
    }

    private List<CurrentHeadcountProductivity> currentProductivities() {
        return List.of(
                CurrentHeadcountProductivity
                        .builder()
                        .abilityLevel(1)
                        .date(A_DATE_UTC)
                        .isActive(true)
                        .productivity(68)
                        .productivityMetricUnit(UNITS_PER_HOUR)
                        .processName(PICKING)
                        .logisticCenterId(WAREHOUSE_ID)
                        .workflow(FBM_WMS_OUTBOUND)
                        .build()
        );
    }

    private static Stream<Arguments> getSupportedEntitites() {
        return Stream.of(
                Arguments.of(PRODUCTIVITY, true),
                Arguments.of(HEADCOUNT, false),
                Arguments.of(THROUGHPUT, false)
        );
    }

    private void outputPropertiesEqualTo(final EntityOutput entityOutput,
                                         final ProcessName processName,
                                         final Source source,
                                         final int quantity) {

        assertEquals(processName, entityOutput.getProcessName());
        assertEquals(source, entityOutput.getSource());
        assertEquals(quantity, entityOutput.getValue());
        assertEquals(UNITS_PER_HOUR, entityOutput.getMetricUnit());
        assertEquals(FBM_WMS_OUTBOUND, entityOutput.getWorkflow());
    }
}
