package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.GetHeadcountEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.GetProductivityEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.output.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.GetSimulationThroughputUseCase;
import com.mercadolibre.planning.model.api.web.controller.request.EntityType;
import com.mercadolibre.planning.model.api.web.controller.request.QuantityByDate;
import com.mercadolibre.planning.model.api.web.controller.request.Source;
import com.mercadolibre.planning.model.api.web.controller.simulation.Simulation;
import com.mercadolibre.planning.model.api.web.controller.simulation.SimulationEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockGetThroughputEntityInput;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockHeadcountEntityOutput;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockProductivityEntityOutput;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.FORECAST;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.SIMULATION;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetSimulationThroughputUseCaseTest {

    @Mock
    private GetProductivityEntityUseCase getProductivityEntityUseCase;

    @Mock
    private GetHeadcountEntityUseCase getHeadcountEntityUseCase;

    @InjectMocks
    private GetSimulationThroughputUseCase getSimulationThroughputUseCase;

    @Test
    @DisplayName("Get throughput entity when source is simulation")
    public void testGetSimulationThroughputOk() {
        // GIVEN
        when(getHeadcountEntityUseCase.execute(any()))
                .thenReturn(mockHeadcountEntityOutput());

        when(getProductivityEntityUseCase.execute(any()))
                .thenReturn(mockProductivityEntityOutput());

        final List<Simulation> simulations = mockSimulations();
        final GetEntityInput input = mockGetThroughputEntityInput(FORECAST, simulations);

        // WHEN
        final List<EntityOutput> output = getSimulationThroughputUseCase.execute(input);

        // THEN
        assertEquals(4, output.size());
        checkOutputProperties(output.get(0), A_DATE_UTC, PICKING, 800, SIMULATION);
        checkOutputProperties(output.get(1), A_DATE_UTC.plusHours(1), PICKING, 1400, SIMULATION);
        checkOutputProperties(output.get(2), A_DATE_UTC, PACKING, 5100, FORECAST);
        checkOutputProperties(output.get(3), A_DATE_UTC.plusHours(1), PACKING, 3000, SIMULATION);
    }

    @ParameterizedTest
    @DisplayName("Only supports throughput entity")
    @MethodSource("getSupportedEntitites")
    public void testSupportEntityTypeOk(final EntityType entityType,
                                        final boolean shouldBeSupported) {
        // WHEN
        final boolean isSupported = getSimulationThroughputUseCase.supportsEntityType(entityType);

        // THEN
        assertEquals(shouldBeSupported, isSupported);
    }

    private void checkOutputProperties(final EntityOutput output,
                                       final ZonedDateTime date,
                                       final ProcessName processName,
                                       final int quantity,
                                       final Source source) {
        assertEquals(date, output.getDate());
        assertEquals(processName, output.getProcessName());
        assertEquals(quantity, output.getValue());
        assertEquals(source, output.getSource());
        assertEquals(UNITS_PER_HOUR, output.getMetricUnit());
        assertEquals(FBM_WMS_OUTBOUND, output.getWorkflow());
    }

    private static Stream<Arguments> getSupportedEntitites() {
        return Stream.of(
                Arguments.of(THROUGHPUT, true),
                Arguments.of(PRODUCTIVITY, false),
                Arguments.of(HEADCOUNT, false)
        );
    }

    private List<Simulation> mockSimulations() {
        return List.of(
                new Simulation(PICKING, List.of(
                        new SimulationEntity(HEADCOUNT,
                                List.of(new QuantityByDate(A_DATE_UTC, 10),
                                        new QuantityByDate(A_DATE_UTC.plusHours(1), 20))),
                        new SimulationEntity(PRODUCTIVITY,
                                List.of(new QuantityByDate(A_DATE_UTC, 80))))
                ),
                new Simulation(PACKING, singletonList(
                        new SimulationEntity(PRODUCTIVITY,
                                singletonList(new QuantityByDate(A_DATE_UTC.plusHours(1), 100))))
                )
        );
    }
}
