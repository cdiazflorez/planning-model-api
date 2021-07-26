package com.mercadolibre.planning.model.api.usecase.cycletime.get;

import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeInput;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeUseCase;
import com.mercadolibre.planning.model.api.exception.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class GetCycleTimeUseCaseTest {

    @InjectMocks
    private GetCycleTimeUseCase useCase;

    @Test
    @DisplayName("Get the cycle time of CPT")
    public void testGetCycleTimeKey() {
        // GIVEN
        final GetCycleTimeInput input = GetCycleTimeInput.builder()
                .cptDate(A_DATE_UTC)
                .configurations(List.of(Configuration.builder()
                        .logisticCenterId(WAREHOUSE_ID)
                        .metricUnit(MINUTES)
                        .value(100)
                        .key("cycle_time_17_00")
                        .build()))
                .build();

        // WHEN
        final Configuration configuration = useCase.execute(input);

        // THEN
        assertEquals(WAREHOUSE_ID, configuration.getLogisticCenterId());
        assertEquals(MINUTES, configuration.getMetricUnit());
        assertEquals(100, configuration.getValue());
        assertEquals("cycle_time_17_00", configuration.getKey());
    }

    @Test
    @DisplayName("Get the default processing time when there is no cycle time for CPT")
    public void testGetProcessingTimeKey() {
        // GIVEN
        final GetCycleTimeInput input = GetCycleTimeInput.builder()
                .cptDate(A_DATE_UTC)
                .configurations(List.of(Configuration.builder()
                        .logisticCenterId(WAREHOUSE_ID)
                        .metricUnit(MINUTES)
                        .value(100)
                        .key("cycle_time_10_00")
                        .build(),
                        Configuration.builder()
                                .logisticCenterId(WAREHOUSE_ID)
                                .metricUnit(MINUTES)
                                .value(240)
                                .key("processing_time")
                                .build()))
                .build();

        // WHEN
        final Configuration configuration = useCase.execute(input);

        // THEN
        assertEquals(WAREHOUSE_ID, configuration.getLogisticCenterId());
        assertEquals(MINUTES, configuration.getMetricUnit());
        assertEquals(240, configuration.getValue());
        assertEquals("processing_time", configuration.getKey());
    }

    @Test
    @DisplayName("Throw an exception when there is no configuration")
    public void testThrowExceptionWhenNoConfiguration() {
        // GIVEN
        final GetCycleTimeInput input = GetCycleTimeInput.builder()
                .cptDate(A_DATE_UTC)
                .configurations(emptyList())
                .build();

        // WHEN - THEN
        assertThrows(EntityNotFoundException.class, () -> useCase.execute(input));
    }

}
