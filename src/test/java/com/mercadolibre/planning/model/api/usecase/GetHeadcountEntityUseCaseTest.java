package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ProcessingDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.GetHeadcountEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.output.GetEntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.output.GetHeadcountOutput;
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

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.ACTIVE_WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockGetHeadcountEntityInput;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.FORECAST;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.SIMULATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetHeadcountEntityUseCaseTest {

    @Mock
    private ProcessingDistributionRepository processingDistRepository;

    @InjectMocks
    private GetHeadcountEntityUseCase getHeadcountEntityUseCase;

    @Test
    @DisplayName("Get headcount entity when source is forecast")
    public void testGetHeadcountOk() {
        // GIVEN
        final GetEntityInput input = mockGetHeadcountEntityInput(FORECAST);
        when(processingDistRepository.findByWarehouseIdAndWorkflowAndTypeAndDateInRange("ARBA01",
                FBM_WMS_OUTBOUND, ACTIVE_WORKERS, A_DATE_UTC, A_DATE_UTC.plusDays(2)))
                .thenReturn(processingDistributions());

        // WHEN
        final List<GetEntityOutput> output = getHeadcountEntityUseCase.execute(input);

        // THEN
        final GetHeadcountOutput output1 = (GetHeadcountOutput) output.get(0);
        assertEquals(A_DATE_UTC, output1.getDate());
        assertEquals(PICKING, output1.getProcessName());
        assertEquals(100, output1.getValue());
        assertEquals(WORKERS, output1.getMetricUnit());
        assertEquals(FORECAST, output1.getSource());
        assertEquals(FBM_WMS_OUTBOUND, output1.getWorkflow());

        final GetHeadcountOutput output2 = (GetHeadcountOutput) output.get(1);
        assertEquals(A_DATE_UTC.plusHours(1), output2.getDate());
        assertEquals(PICKING, output2.getProcessName());
        assertEquals(120, output2.getValue());
        assertEquals(WORKERS, output2.getMetricUnit());
        assertEquals(FORECAST, output2.getSource());
        assertEquals(FBM_WMS_OUTBOUND, output2.getWorkflow());
    }

    @Test
    @DisplayName("Get headcount entity when source is simulation")
    public void testGetHeadcountFromSourceSimulation() {
        // GIVEN
        final GetEntityInput input = mockGetHeadcountEntityInput(SIMULATION);

        // WHEN
        final List<GetEntityOutput> output = getHeadcountEntityUseCase.execute(input);

        // THEN
        assertThat(output).isEmpty();
    }

    private List<ProcessingDistribution> processingDistributions() {
        return List.of(
                new ProcessingDistribution(1, A_DATE_UTC, PICKING,
                        100, WORKERS, ACTIVE_WORKERS, null),
                new ProcessingDistribution(2, A_DATE_UTC.plusHours(1), PICKING,
                        120, WORKERS, ACTIVE_WORKERS, null)
        );
    }

    @ParameterizedTest
    @DisplayName("Only supports headcount entity")
    @MethodSource("getSupportedEntitites")
    public void testSupportEntityTypeOk(final EntityType entityType,
                                        final boolean shouldBeSupported) {
        // WHEN
        final boolean isSupported = getHeadcountEntityUseCase.supportsEntityType(entityType);

        // THEN
        assertEquals(shouldBeSupported, isSupported);
    }

    private static Stream<Arguments> getSupportedEntitites() {
        return Stream.of(
                Arguments.of(HEADCOUNT, true),
                Arguments.of(PRODUCTIVITY, false),
                Arguments.of(THROUGHPUT, false)
        );
    }
}
