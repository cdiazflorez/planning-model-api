package com.mercadolibre.planning.model.api.usecase.projection.capacity;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.Backlog;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CalculateCptProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.GetDeliveryPromiseProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetDeliveryPromiseProjectionInput;
import com.mercadolibre.planning.model.api.usecase.ProcessingDistributionViewImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetDeliveryPromiseProjectionUseCaseTest {

    private static final ZonedDateTime CPT_1 = ZonedDateTime.now().plusHours(1);
    private static final ZonedDateTime CPT_2 = ZonedDateTime.now().plusHours(2);

    @InjectMocks
    private GetDeliveryPromiseProjectionUseCase useCase;

    @Mock
    private CalculateCptProjectionUseCase projectionUseCase;

    @Mock
    private ProcessingDistributionRepository processingDistRepository;

    @Mock
    private GetForecastUseCase getForecastUseCase;

    @Test
    public void testExecute() {
        //GIVEN
        final GetDeliveryPromiseProjectionInput input = GetDeliveryPromiseProjectionInput.builder()
                .warehouseId("ARBA01")
                .workflow(Workflow.FBM_WMS_OUTBOUND)
                .dateFrom(ZonedDateTime.now())
                .dateTo(ZonedDateTime.now().plusDays(1))
                .backlog(List.of(new Backlog(CPT_1, 100), new Backlog(CPT_2, 200)))
                .build();

        final List<Long> forecastIds = List.of(1L, 2L);
        final List<ProcessingDistributionView> maxCapacity = mockProcessingDist();

        when(getForecastUseCase.execute(GetForecastInput.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getWarehouseId())
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .build())).thenReturn(forecastIds);

        when(processingDistRepository
                .findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                        Set.of(ProcessingType.MAX_CAPACITY.name()),
                        List.of(ProcessName.GLOBAL.toJson()),
                        input.getDateFrom(),
                        input.getDateTo(),
                        forecastIds
                )).thenReturn(maxCapacity);

        when(projectionUseCase.execute(CptProjectionInput.builder()
                .capacity(maxCapacity.stream().collect(Collectors.toMap(
                        o -> ZonedDateTime.ofInstant(o.getDate().toInstant(), UTC),
                        o -> (int) o.getQuantity(),
                        (intA, intB) -> intB,
                        TreeMap::new)))
                .backlog(input.getBacklog())
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .planningUnits(Collections.emptyList())
                .build())
        ).thenReturn(mockProjectionResponse());

        //WHEN
        final List<CptProjectionOutput> response = useCase.execute(input);

        //THEN
        assertEquals(mockProjectionResponse(), response);
    }

    private List<ProcessingDistributionView> mockProcessingDist() {
        return List.of(
                ProcessingDistributionViewImpl.builder()
                        .date(Date.from(LocalDateTime.now().plusHours(1).toInstant(UTC)))
                        .quantity(100L)
                        .build(),
                ProcessingDistributionViewImpl.builder()
                        .date(Date.from(LocalDateTime.now().plusHours(2).toInstant(UTC)))
                        .quantity(100L)
                        .build()
        );
    }

    private List<CptProjectionOutput> mockProjectionResponse() {
        return List.of(
                new CptProjectionOutput(CPT_1, null, 0),
                new CptProjectionOutput(CPT_2, null,100));
    }
}
