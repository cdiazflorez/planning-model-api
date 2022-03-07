package com.mercadolibre.planning.model.api.usecase.cycletime.get;

import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.get.GetConfigurationsUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeInput;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeService;
import com.mercadolibre.planning.model.api.exception.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetGetCptOutputUseCaseTest {

    @InjectMocks
    private GetCycleTimeService useCase;

    @Mock
    private GetConfigurationsUseCase getConfigurationsUseCase;

    @Test
    @DisplayName("Get the cycle time of CPT")
    public void testGetCycleTimeKey() {
        // GIVEN
        when(getConfigurationsUseCase.execute("ARBA01"))
                .thenReturn(List.of(
                        Configuration.builder()
                                .logisticCenterId(WAREHOUSE_ID)
                                .value(100)
                                .metricUnit(MINUTES)
                                .key("cycle_time_17_00")
                                .build()));

        final GetCycleTimeInput input = new GetCycleTimeInput(WAREHOUSE_ID, List.of(A_DATE_UTC));

        // WHEN
        final Map<ZonedDateTime, Long> ctByDateOut = useCase.execute(input);

        // THEN
        assertEquals(100, ctByDateOut.get(A_DATE_UTC));
    }

    @Test
    @DisplayName("Get the default cycle time when there is no cycle time for CPT")
    public void testGetDefaultCycleTimeKey() {
        // GIVEN
        when(getConfigurationsUseCase.execute("ARBA01"))
                .thenReturn(List.of(
                        Configuration.builder()
                                .logisticCenterId(WAREHOUSE_ID)
                                .metricUnit(MINUTES)
                                .value(100)
                                .key("cycle_time_10_00")
                                .build(),
                        Configuration.builder()
                                .logisticCenterId(WAREHOUSE_ID)
                                .metricUnit(MINUTES)
                                .value(240)
                                .key("cycle_time")
                                .build()));

        final GetCycleTimeInput input = new GetCycleTimeInput(WAREHOUSE_ID, List.of(A_DATE_UTC));

        // WHEN
        final Map<ZonedDateTime, Long> ctByDateOut = useCase.execute(input);

        // THEN
        assertEquals(240, ctByDateOut.get(A_DATE_UTC));
    }

    @Test
    @DisplayName("Throw an exception when there is no configuration")
    public void testThrowExceptionWhenNoConfiguration() {
        // GIVEN
        when(getConfigurationsUseCase.execute("ARBA01"))
                .thenReturn(emptyList());

        final GetCycleTimeInput input = new GetCycleTimeInput(WAREHOUSE_ID, List.of(A_DATE_UTC));

        // WHEN - THEN
        assertThrows(EntityNotFoundException.class, () -> useCase.execute(input));
    }
}
