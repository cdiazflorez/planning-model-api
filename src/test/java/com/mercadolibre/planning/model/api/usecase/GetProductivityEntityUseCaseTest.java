package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.HeadcountProductivityRepository;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountProductivity;
import com.mercadolibre.planning.model.api.domain.usecase.GetProductivityEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.output.GetEntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.output.GetProductivityOutput;
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

import java.time.OffsetTime;
import java.util.List;
import java.util.stream.Stream;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockGetProductivityEntityInput;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.FORECAST;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.SIMULATION;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetProductivityEntityUseCaseTest {

    private static final OffsetTime AN_OFFSET_TIME = OffsetTime.parse("14:00:00-03:00")
            .withOffsetSameInstant(UTC);

    @Mock
    private HeadcountProductivityRepository productivityRepository;

    @InjectMocks
    private GetProductivityEntityUseCase getProductivityEntityUseCase;

    @Test
    @DisplayName("Get productivity entity when source is forecast")
    public void testGetProductivityOk() {
        // GIVEN
        final GetEntityInput input = mockGetProductivityEntityInput(FORECAST);
        when(productivityRepository.findByWarehouseIdAndWorkflowAndProcessNameAndDayTimeInRange(
                "ARBA01", FBM_WMS_OUTBOUND, List.of(PICKING, PACKING),
                AN_OFFSET_TIME, AN_OFFSET_TIME.plusHours(1))).thenReturn(productivities());

        // WHEN
        final List<GetEntityOutput> output = getProductivityEntityUseCase.execute(input);

        // THEN
        final GetProductivityOutput output1 = (GetProductivityOutput) output.get(0);
        assertEquals(PICKING, output1.getProcessName());
        assertEquals(80.0, output1.getValue());
        assertEquals(UNITS_PER_HOUR, output1.getMetricUnit());
        assertEquals(FORECAST, output1.getSource());
        assertEquals(FBM_WMS_OUTBOUND, output1.getWorkflow());

        final GetProductivityOutput output2 = (GetProductivityOutput) output.get(1);
        assertEquals(PICKING, output2.getProcessName());
        assertEquals(85.0, output2.getValue());
        assertEquals(UNITS_PER_HOUR, output2.getMetricUnit());
        assertEquals(FORECAST, output2.getSource());
        assertEquals(FBM_WMS_OUTBOUND, output2.getWorkflow());

        final GetProductivityOutput output3 = (GetProductivityOutput) output.get(2);
        assertEquals(PACKING, output3.getProcessName());
        assertEquals(90, output3.getValue());
        assertEquals(UNITS_PER_HOUR, output3.getMetricUnit());
        assertEquals(FORECAST, output3.getSource());
        assertEquals(FBM_WMS_OUTBOUND, output3.getWorkflow());

        final GetProductivityOutput output4 = (GetProductivityOutput) output.get(3);
        assertEquals(PACKING, output4.getProcessName());
        assertEquals(92.5, output4.getValue());
        assertEquals(UNITS_PER_HOUR, output4.getMetricUnit());
        assertEquals(FORECAST, output4.getSource());
        assertEquals(FBM_WMS_OUTBOUND, output4.getWorkflow());
    }

    @Test
    @DisplayName("Get productivity entity when source is simulation")
    public void testGetProductivityFromSourceSimulation() {
        // GIVEN
        final GetEntityInput input = mockGetProductivityEntityInput(SIMULATION);

        // WHEN
        final List<GetEntityOutput> output = getProductivityEntityUseCase.execute(input);

        // THEN
        assertThat(output).isEmpty();
    }

    private List<HeadcountProductivity> productivities() {
        return List.of(
                new HeadcountProductivity(1, AN_OFFSET_TIME, PICKING,
                        80, UNITS_PER_HOUR, 1, null),
                new HeadcountProductivity(2, AN_OFFSET_TIME.plusHours(1), PICKING,
                        85, UNITS_PER_HOUR, 1, null),
                new HeadcountProductivity(3, AN_OFFSET_TIME, PACKING,
                        90, UNITS_PER_HOUR, 1, null),
                new HeadcountProductivity(4, AN_OFFSET_TIME.plusHours(1), PACKING,
                        92.5, UNITS_PER_HOUR, 1, null)
        );
    }

    @ParameterizedTest
    @DisplayName("Only supports productivity entity")
    @MethodSource("getSupportedEntitites")
    public void testSupportEntityTypeOk(final EntityType entityType,
                                        final boolean shouldBeSupported) {
        // WHEN
        final boolean isSupported = getProductivityEntityUseCase.supportsEntityType(entityType);

        // THEN
        assertEquals(shouldBeSupported, isSupported);
    }

    private static Stream<Arguments> getSupportedEntitites() {
        return Stream.of(
                Arguments.of(PRODUCTIVITY, true),
                Arguments.of(HEADCOUNT, false),
                Arguments.of(THROUGHPUT, false)
        );
    }
}
