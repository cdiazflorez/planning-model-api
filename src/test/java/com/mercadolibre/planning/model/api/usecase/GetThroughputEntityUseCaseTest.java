package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.domain.usecase.GetHeadcountEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.GetProductivityEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.GetThroughputEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.output.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.output.ThroughputOutput;
import com.mercadolibre.planning.model.api.web.controller.request.EntityType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetThroughputEntityUseCaseTest {

    @Mock
    private GetProductivityEntityUseCase getProductivityEntityUseCase;

    @Mock
    private GetHeadcountEntityUseCase getHeadcountEntityUseCase;

    @InjectMocks
    private GetThroughputEntityUseCase getThroughputEntityUseCase;

    @Test
    @DisplayName("Get throughput entity when source is forecast")
    public void testGetForecastThroughputOk() {
        // GIVEN
        when(getHeadcountEntityUseCase.execute(any()))
                .thenReturn(mockHeadcountEntityOutput());

        when(getProductivityEntityUseCase.execute(any()))
                .thenReturn(mockProductivityEntityOutput());

        final GetEntityInput input = mockGetThroughputEntityInput(FORECAST);

        // WHEN
        final List<EntityOutput> output = getThroughputEntityUseCase.execute(input);

        // THEN
        assertEquals(4, output.size());
        final ThroughputOutput output1 = (ThroughputOutput) output.get(0);
        assertEquals(A_DATE_UTC, output1.getDate());
        assertEquals(PICKING, output1.getProcessName());
        assertEquals(4000.0, output1.getValue());
        assertEquals(UNITS_PER_HOUR, output1.getMetricUnit());
        assertEquals(FORECAST, output1.getSource());
        assertEquals(FBM_WMS_OUTBOUND, output1.getWorkflow());

        final ThroughputOutput output2 = (ThroughputOutput) output.get(1);
        assertEquals(A_DATE_UTC.plusHours(1), output2.getDate());
        assertEquals(PICKING, output2.getProcessName());
        assertEquals(2800, output2.getValue());
        assertEquals(UNITS_PER_HOUR, output2.getMetricUnit());
        assertEquals(FORECAST, output2.getSource());
        assertEquals(FBM_WMS_OUTBOUND, output2.getWorkflow());

        final ThroughputOutput output3 = (ThroughputOutput) output.get(2);
        assertEquals(A_DATE_UTC, output3.getDate());
        assertEquals(PACKING, output3.getProcessName());
        assertEquals(5100, output3.getValue());
        assertEquals(UNITS_PER_HOUR, output3.getMetricUnit());
        assertEquals(FORECAST, output3.getSource());
        assertEquals(FBM_WMS_OUTBOUND, output3.getWorkflow());

        final ThroughputOutput output4 = (ThroughputOutput) output.get(3);
        assertEquals(A_DATE_UTC.plusHours(1), output4.getDate());
        assertEquals(PACKING, output4.getProcessName());
        assertEquals(2715.0, output4.getValue());
        assertEquals(UNITS_PER_HOUR, output4.getMetricUnit());
        assertEquals(FORECAST, output4.getSource());
        assertEquals(FBM_WMS_OUTBOUND, output4.getWorkflow());
    }

    @Test
    @DisplayName("Get throughput entity when source is simulation")
    public void testGetThroughputFromSourceSimulation() {
        // GIVEN
        final GetEntityInput input = mockGetThroughputEntityInput(SIMULATION);

        // WHEN
        final List<EntityOutput> output = getThroughputEntityUseCase.execute(input);

        // THEN
        verifyZeroInteractions(getProductivityEntityUseCase);
        verifyZeroInteractions(getHeadcountEntityUseCase);
        assertThat(output).isEmpty();
    }

    @ParameterizedTest
    @DisplayName("Only supports throughput entity")
    @MethodSource("getSupportedEntitites")
    public void testSupportEntityTypeOk(final EntityType entityType,
                                        final boolean shouldBeSupported) {
        // WHEN
        final boolean isSupported = getThroughputEntityUseCase.supportsEntityType(entityType);

        // THEN
        assertEquals(shouldBeSupported, isSupported);
    }

    private static Stream<Arguments> getSupportedEntitites() {
        return Stream.of(
                Arguments.of(THROUGHPUT, true),
                Arguments.of(PRODUCTIVITY, false),
                Arguments.of(HEADCOUNT, false)
        );
    }
}
