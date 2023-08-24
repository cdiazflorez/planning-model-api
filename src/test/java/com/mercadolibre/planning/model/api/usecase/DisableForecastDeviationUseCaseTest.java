package com.mercadolibre.planning.model.api.usecase;

import static com.mercadolibre.planning.model.api.domain.entity.DeviationType.UNITS;
import static com.mercadolibre.planning.model.api.domain.entity.Path.COLLECT;
import static com.mercadolibre.planning.model.api.domain.entity.Path.FTL;
import static com.mercadolibre.planning.model.api.domain.entity.Path.PRIVATE;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockCurrentForecastDeviationWithPath;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockDisableForecastDeviationInput;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockDisableForecastDeviationInputWithAllArgs;
import static java.time.ZonedDateTime.now;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Set.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.CurrentForecastDeviationRepository;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.disable.DisableForecastDeviationInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.disable.DisableForecastDeviationUseCase;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DisableForecastDeviationUseCaseTest {

    private static Integer deviationDisableOneInputTest = 1;

    private static Integer deviationDisable = 2;

    @InjectMocks
    private DisableForecastDeviationUseCase useCase;

    @Mock
    private CurrentForecastDeviationRepository deviationRepository;

    @Test
    public void testDisableForecastDeviationOneInput() {

        final ZonedDateTime currentDate = Instant.now().truncatedTo(HOURS).atZone(ZoneOffset.UTC);

        // GIVEN
        final List<DisableForecastDeviationInput> arguments = List.of(
            mockDisableForecastDeviationInputWithAllArgs(
                FBM_WMS_INBOUND,
                UNITS,
                List.of(PRIVATE))
        );

        when(deviationRepository.findByLogisticCenterIdAndIsActiveTrueAndWorkflowInAndPathAndTypeAndDateToIsGreaterThan(
            WAREHOUSE_ID,
            of(FBM_WMS_INBOUND),
            PRIVATE,
            UNITS,
            currentDate
        ))
            .thenReturn(List.of(mockCurrentForecastDeviationWithPath(PRIVATE)));

        final int output = useCase.execute(arguments, currentDate);

        //THEN
        verify(deviationRepository).findByLogisticCenterIdAndIsActiveTrueAndWorkflowInAndPathAndTypeAndDateToIsGreaterThan(
            WAREHOUSE_ID, of(FBM_WMS_INBOUND), PRIVATE, UNITS, currentDate);
        assertEquals(deviationDisableOneInputTest, output);
    }

    @Test
    public void testDisableForecastDeviationMultipleInputs() {
        // GIVEN
        final ZonedDateTime currentDate = Instant.now().truncatedTo(HOURS).atZone(ZoneOffset.UTC);

        final List<DisableForecastDeviationInput> arguments = List.of(
            mockDisableForecastDeviationInputWithAllArgs(
                FBM_WMS_INBOUND,
                UNITS,
                List.of(FTL, COLLECT)),
            mockDisableForecastDeviationInputWithAllArgs(
                FBM_WMS_INBOUND,
                UNITS,
                List.of(PRIVATE))
        );

        when(deviationRepository.findByLogisticCenterIdAndIsActiveTrueAndWorkflowInAndPathAndTypeAndDateToIsGreaterThan(
            WAREHOUSE_ID,
            of(FBM_WMS_INBOUND),
            FTL,
            UNITS,
            currentDate
        ))
            .thenReturn(List.of());
        when(deviationRepository.findByLogisticCenterIdAndIsActiveTrueAndWorkflowInAndPathAndTypeAndDateToIsGreaterThan(
            WAREHOUSE_ID,
            of(FBM_WMS_INBOUND),
            COLLECT,
            UNITS,
            currentDate
        ))
            .thenReturn(List.of(mockCurrentForecastDeviationWithPath(COLLECT)));
        when(deviationRepository.findByLogisticCenterIdAndIsActiveTrueAndWorkflowInAndPathAndTypeAndDateToIsGreaterThan(
            WAREHOUSE_ID,
            of(FBM_WMS_INBOUND),
            PRIVATE,
            UNITS,
            currentDate
        ))
            .thenReturn(List.of(mockCurrentForecastDeviationWithPath(PRIVATE)));

        final int output = useCase.execute(arguments, currentDate);

        //THEN

        verify(deviationRepository).findByLogisticCenterIdAndIsActiveTrueAndWorkflowInAndPathAndTypeAndDateToIsGreaterThan(
            WAREHOUSE_ID, of(FBM_WMS_INBOUND), FTL, UNITS, currentDate);
        verify(deviationRepository).findByLogisticCenterIdAndIsActiveTrueAndWorkflowInAndPathAndTypeAndDateToIsGreaterThan(
            WAREHOUSE_ID, of(FBM_WMS_INBOUND), COLLECT, UNITS, currentDate);
        verify(deviationRepository).findByLogisticCenterIdAndIsActiveTrueAndWorkflowInAndPathAndTypeAndDateToIsGreaterThan(
            WAREHOUSE_ID, of(FBM_WMS_INBOUND), PRIVATE, UNITS, currentDate);
        assertEquals(deviationDisable, output);
    }

    @Test
    public void testDisableForecastDeviationOk() {
        // GIVEN
        final ZonedDateTime currentDate = Instant.now().truncatedTo(HOURS).atZone(ZoneOffset.UTC);

        final DisableForecastDeviationInput input = mockDisableForecastDeviationInput(FBM_WMS_OUTBOUND, UNITS);

        when(deviationRepository.findByLogisticCenterIdAndIsActiveTrueAndWorkflowInAndPathAndTypeAndDateToIsGreaterThan(
            WAREHOUSE_ID,
            of(FBM_WMS_OUTBOUND),
            null,
            UNITS,
            currentDate
        )).thenReturn(mockCurrentForecastDeviation(true, now().minusMinutes(15)));

        final List<CurrentForecastDeviation> toSave = mockCurrentForecastDeviation(false, now());
        when(deviationRepository.saveAll(any(List.class)))
                .thenReturn(toSave);

        final int output = useCase.execute(List.of(input), currentDate);

        // THEN
        verify(deviationRepository).findByLogisticCenterIdAndIsActiveTrueAndWorkflowInAndPathAndTypeAndDateToIsGreaterThan(
            WAREHOUSE_ID, of(FBM_WMS_OUTBOUND), null, UNITS, currentDate);
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
