package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.remove.DeleteForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.remove.DeleteForecastUseCase;
import com.mercadolibre.planning.model.api.exception.BadRequestException;
import com.mercadolibre.planning.model.api.gateway.ForecastGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import static com.mercadolibre.planning.model.api.util.TestUtils.LIMIT;

@ExtendWith(MockitoExtension.class)
public class DeleteForecastUseCaseTest {

    @Mock
    private ForecastGateway forecastGateway;

    @InjectMocks
    private DeleteForecastUseCase deleteForecastUseCase;

    @Test
    public void deleteForecastOk() {
        // GIVEN
        final DeleteForecastInput input = new DeleteForecastInput(Workflow.FBM_WMS_OUTBOUND, 1, LIMIT);

        when(forecastGateway.deleteOlderThan(
                eq(Workflow.FBM_WMS_OUTBOUND),
                any(ZonedDateTime.class),
                eq(LIMIT))
        ).thenReturn(5);

        // WHEN
        final int result = deleteForecastUseCase.execute(input);

        // THEN
        assertEquals(5, result);
    }

    @Test
    public void deleteForecastErr() {
        // GIVEN
        final DeleteForecastInput input = new DeleteForecastInput(Workflow.FBM_WMS_OUTBOUND, -1, LIMIT);

        // WHEN
        final Executable executable = () -> deleteForecastUseCase.execute(input);

        // THEN
        assertThrows(BadRequestException.class, executable);
    }
}
