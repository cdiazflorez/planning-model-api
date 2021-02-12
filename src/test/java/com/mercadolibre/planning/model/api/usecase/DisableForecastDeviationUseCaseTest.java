package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.CurrentForecastDeviationRepository;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.disable.DisableForecastDeviationInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.disable.DisableForecastDeviationUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockDisableForecastDeviationInput;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DisableForecastDeviationUseCaseTest {

    @InjectMocks
    private DisableForecastDeviationUseCase useCase;

    @Mock
    private CurrentForecastDeviationRepository deviationRepository;

    @Test
    public void testDisableForecastDeviationOk() {
        // GIVEN

        final DisableForecastDeviationInput input = mockDisableForecastDeviationInput();

        when(deviationRepository.findByLogisticCenterId(WAREHOUSE_ID))
                .thenReturn(mockCurrentForecastDeviation(true));

        final List<CurrentForecastDeviation> toSave = mockCurrentForecastDeviation(false);
        when(deviationRepository.saveAll(toSave)).thenReturn(toSave);

        // WHEN
        final int output = useCase.execute(input);

        // THEN

        verify(deviationRepository).findByLogisticCenterId(WAREHOUSE_ID);
        verify(deviationRepository).saveAll(toSave);
        assertEquals(2, output);
    }

    private List<CurrentForecastDeviation> mockCurrentForecastDeviation(final boolean active) {
        return List.of(
                CurrentForecastDeviation
                        .builder()
                        .logisticCenterId(WAREHOUSE_ID)
                        .workflow(FBM_WMS_OUTBOUND)
                        .id(1L)
                        .isActive(active)
                        .build(),
                CurrentForecastDeviation
                        .builder()
                        .logisticCenterId(WAREHOUSE_ID)
                        .workflow(FBM_WMS_OUTBOUND)
                        .id(2L)
                        .isActive(active)
                        .build()
            );
    }
}
