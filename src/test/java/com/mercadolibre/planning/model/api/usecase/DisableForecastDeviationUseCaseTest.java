package com.mercadolibre.planning.model.api.usecase;

import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockDisableForecastDeviationInput;
import static java.time.ZonedDateTime.now;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.CurrentForecastDeviationRepository;
import com.mercadolibre.planning.model.api.domain.entity.DeviationType;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.disable.DisableForecastDeviationInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.disable.DisableForecastDeviationUseCase;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DisableForecastDeviationUseCaseTest {

    private static Integer deviationDisable = 2;

    @InjectMocks
    private DisableForecastDeviationUseCase useCase;

    @Mock
    private CurrentForecastDeviationRepository deviationRepository;

    @Test
    public void testDisableForecastDeviationOk() {
        // GIVEN
        final DisableForecastDeviationInput input = mockDisableForecastDeviationInput(FBM_WMS_OUTBOUND, DeviationType.UNITS);

        // WHEN
        when(deviationRepository.findByLogisticCenterIdAndWorkflowAndIsActiveTrue(WAREHOUSE_ID, FBM_WMS_OUTBOUND))
                .thenReturn(mockCurrentForecastDeviation(true, now().minusMinutes(15)));

        final List<CurrentForecastDeviation> toSave = mockCurrentForecastDeviation(false, now());
        when(deviationRepository.saveAll(any(List.class)))
                .thenReturn(toSave);

        final int output = useCase.execute(input);

        // THEN
        verify(deviationRepository).findByLogisticCenterIdAndWorkflowAndIsActiveTrue(WAREHOUSE_ID, FBM_WMS_OUTBOUND);
        verify(deviationRepository).saveAll(any(List.class));
        assertEquals(deviationDisable, output);
    }

    private List<CurrentForecastDeviation> mockCurrentForecastDeviation(final boolean active,
                                                                        final ZonedDateTime date) {
        return List.of(
                CurrentForecastDeviation
                        .builder()
                        .logisticCenterId(WAREHOUSE_ID)
                        .workflow(FBM_WMS_OUTBOUND)
                        .id(1L)
                        .isActive(active)
                        .lastUpdated(date)
                        .dateCreated(date.truncatedTo(HOURS).minusMinutes(5))
                        .build(),
                CurrentForecastDeviation
                        .builder()
                        .logisticCenterId(WAREHOUSE_ID)
                        .workflow(FBM_WMS_OUTBOUND)
                        .id(2L)
                        .isActive(active)
                        .lastUpdated(date)
                        .dateCreated(date.truncatedTo(HOURS).minusMinutes(5))
                        .build()
        );
    }
}
