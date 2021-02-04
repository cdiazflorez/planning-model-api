package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.CurrentForecastDeviationRepository;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.save.SaveForecastDeviationInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.save.SaveForecastDeviationUseCase;
import com.mercadolibre.planning.model.api.web.controller.deviation.response.DeviationResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockSaveForecastDeviationInput;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SaveForecastDeviationUseCaseTest {

    @InjectMocks
    private SaveForecastDeviationUseCase useCase;

    @Mock
    private CurrentForecastDeviationRepository deviationRepository;

    @Test
    public void testSaveForecastDeviationOk() {
        // GIVEN

        final SaveForecastDeviationInput input = mockSaveForecastDeviationInput();

        final CurrentForecastDeviation saved = CurrentForecastDeviation
                .builder()
                .workflow(FBM_WMS_OUTBOUND)
                .id(1L)
                .build();

        when(deviationRepository.save(any(CurrentForecastDeviation.class))).thenReturn(saved);

        // WHEN
        final DeviationResponse output = useCase.execute(input);

        // THEN

        verify(deviationRepository).save(any(CurrentForecastDeviation.class));
        assertEquals(200L, output.getStatus());
    }
}
