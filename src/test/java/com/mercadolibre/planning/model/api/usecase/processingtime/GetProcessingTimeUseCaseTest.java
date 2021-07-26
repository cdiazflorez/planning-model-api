package com.mercadolibre.planning.model.api.usecase.processingtime;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentProcessingTimeRepository;
import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingTime;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.get.GetConfigurationByKeyUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.get.GetConfigurationInput;
import com.mercadolibre.planning.model.api.domain.usecase.processingtime.get.GetProcessingTimeInput;
import com.mercadolibre.planning.model.api.domain.usecase.processingtime.get.GetProcessingTimeOutput;
import com.mercadolibre.planning.model.api.domain.usecase.processingtime.get.GetProcessingTimeUseCase;
import com.mercadolibre.planning.model.api.exception.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.USER_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetProcessingTimeUseCaseTest {

    @InjectMocks
    private GetProcessingTimeUseCase useCase;

    @Mock
    private CurrentProcessingTimeRepository repository;

    @Mock
    private GetConfigurationByKeyUseCase getConfigurationByKeyUseCase;

    @Test
    @DisplayName("When a deviation was loaded for processing time it must be returned")
    public void testGetProcessingTimeWithDeviationOk() {
        // GIVEN
        final GetProcessingTimeInput input = getGetProcessingTimeInput();

        when(repository.findByWorkflowAndLogisticCenterIdAndIsActiveTrueAndDateBetweenCpt(
                input.getWorkflow(),
                input.getLogisticCenterId()
        )).thenReturn(List.of(
                CurrentProcessingTime.builder()
                        .id(1L)
                        .workflow(FBM_WMS_OUTBOUND)
                        .logisticCenterId(WAREHOUSE_ID)
                        .cptFrom(A_DATE_UTC.minusHours(4))
                        .cptTo(A_DATE_UTC.plusHours(2))
                        .value(300)
                        .metricUnit(MINUTES)
                        .userId(USER_ID)
                        .build()));

        when(getConfigurationByKeyUseCase.execute(
                new GetConfigurationInput(WAREHOUSE_ID, "processing_time"))
        ).thenReturn(Optional.of(Configuration.builder()
                .logisticCenterId(WAREHOUSE_ID)
                .key("processing_time")
                .value(300)
                .metricUnit(MINUTES)
                .build()));

        // WHEN
        final List<GetProcessingTimeOutput> outputs = useCase.execute(input);

        // THEN
        assertNotNull(outputs.get(0));
        assertEquals(FBM_WMS_OUTBOUND, outputs.get(0).getWorkflow());
        assertEquals(WAREHOUSE_ID, outputs.get(0).getLogisticCenterId());
        assertEquals(300, outputs.get(0).getValue());
        assertEquals(MINUTES, outputs.get(0).getMetricUnit());
    }

    @Test
    @DisplayName("When no deviation was loaded for processing time, then default value "
            + "from configuration must be returned")
    public void testGetProcessingTimeWithNoDeviationOk() {
        // GIVEN
        final GetProcessingTimeInput input = getGetProcessingTimeInput();

        when(repository.findByWorkflowAndLogisticCenterIdAndIsActiveTrueAndDateBetweenCpt(
                input.getWorkflow(),
                input.getLogisticCenterId())).thenReturn(List.of());

        when(getConfigurationByKeyUseCase.execute(
                new GetConfigurationInput(WAREHOUSE_ID, "processing_time"))
        ).thenReturn(Optional.of(Configuration.builder()
                        .logisticCenterId(WAREHOUSE_ID)
                        .key("processing_time")
                        .value(360)
                        .metricUnit(MINUTES)
                        .build()
                ));

        // WHEN
        final List<GetProcessingTimeOutput> outputs = useCase.execute(input);

        // THEN
        assertNotNull(outputs.get(0));
        assertEquals(FBM_WMS_OUTBOUND, outputs.get(0).getWorkflow());
        assertEquals(WAREHOUSE_ID, outputs.get(0).getLogisticCenterId());
        assertEquals(360, outputs.get(0).getValue());
        assertEquals(MINUTES, outputs.get(0).getMetricUnit());
    }

    @Test
    @DisplayName("When no configuration for processing_time is loaded we must fail")
    public void testGetProcessingTimeFail() {
        // GIVEN
        final GetProcessingTimeInput input = getGetProcessingTimeInput();

        when(repository.findByWorkflowAndLogisticCenterIdAndIsActiveTrueAndDateBetweenCpt(
                input.getWorkflow(),
                input.getLogisticCenterId())).thenReturn(List.of());

        when(getConfigurationByKeyUseCase.execute(
                new GetConfigurationInput(WAREHOUSE_ID, "processing_time"))
        ).thenThrow(new EntityNotFoundException(
                "CONFIGURATION",
                input.getLogisticCenterId() + "processing_time"
        ));

        // WHEN - THEN
        assertThrows(EntityNotFoundException.class, () -> useCase.execute(input));
    }

    private GetProcessingTimeInput getGetProcessingTimeInput() {
        return GetProcessingTimeInput.builder()
                .logisticCenterId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .cpt(List.of(A_DATE_UTC))
                .build();
    }
}
