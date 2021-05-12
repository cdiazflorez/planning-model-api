package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentProcessingTimeRepository;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingTime;
import com.mercadolibre.planning.model.api.domain.usecase.processingtime.create.CreateProcessingTimeInput;
import com.mercadolibre.planning.model.api.domain.usecase.processingtime.create.CreateProcessingTimeOutput;
import com.mercadolibre.planning.model.api.domain.usecase.processingtime.create.CreateProcessingTimeUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static java.time.ZonedDateTime.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateProcessingTimeUseCaseTest {

    @InjectMocks
    private CreateProcessingTimeUseCase createProcessingTimeUseCase;

    @Mock
    private CurrentProcessingTimeRepository processingTimeRepository;

    @Test
    public void testCreateProcessingTime() {

        // GIVEN
        final CreateProcessingTimeInput input =
                CreateProcessingTimeInput.builder()
                        .value(360)
                        .metricUnit(MetricUnit.MINUTES)
                        .logisticCenterId(WAREHOUSE_ID)
                        .workflow(Workflow.FBM_WMS_OUTBOUND)
                        .cptFrom(parse("2021-01-01T11:00:00Z[UTC]"))
                        .cptTo(parse("2021-01-02T10:00:00Z[UTC]"))
                        .userId(1234)
                        .build();

        final CurrentProcessingTime currentProcessingTimeEntity =
                CurrentProcessingTime.builder()
                        .value(input.getValue())
                        .logisticCenterId(input.getLogisticCenterId())
                        .metricUnit(input.getMetricUnit())
                        .workflow(input.getWorkflow())
                        .cptFrom(input.getCptFrom())
                        .cptTo(input.getCptTo())
                        .isActive(true)
                        .userId(input.getUserId())
                        .build();

        // WHEN
        when(processingTimeRepository.save(currentProcessingTimeEntity))
                .thenReturn(currentProcessingTimeEntity);

        final CreateProcessingTimeOutput response =
                createProcessingTimeUseCase.execute(input);

        assertEquals(response.getValue(), 360);
        assertEquals(response.getMetricUnit(), MetricUnit.MINUTES);
        assertEquals(response.getLogisticCenterId(), WAREHOUSE_ID);
    }
}
