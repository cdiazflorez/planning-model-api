package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntitiesStrategy;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.SearchEntitiesUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.input.SearchEntitiesInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.remainingprocessing.get.GetRemainingProcessingUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.ACTIVE_WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockGetRemainingProcessingOutput;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockHeadcountEntityOutput;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockProductivityEntityOutput;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockThroughputEntityOutput;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.REMAINING_PROCESSING;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.FORECAST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SearchEntitiesUseCaseTest {

    @InjectMocks
    private SearchEntitiesUseCase searchEntitiesUseCase;

    @Mock
    private GetEntitiesStrategy getEntitiesStrategy;

    @Mock
    private GetHeadcountEntityUseCase getHeadcountEntityUseCase;

    @Mock
    private GetProductivityEntityUseCase getProductivityEntityUseCase;

    @Mock
    private GetRemainingProcessingUseCase getRemainingProcessingUseCase;

    @Mock
    private GetThroughputUseCase getThroughputUseCase;

    @Test
    @DisplayName("Search all entities")
    public void testSearchAllEntitiesOk() {
        // GIVEN
        final SearchEntitiesInput input = SearchEntitiesInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .entityTypes(List.of(HEADCOUNT, PRODUCTIVITY, REMAINING_PROCESSING, THROUGHPUT))
                .source(FORECAST)
                .dateFrom(A_DATE_UTC)
                .dateTo(A_DATE_UTC.plusHours(12))
                .processName(List.of(PICKING, PACKING))
                .entityFilters(Map.of(
                        HEADCOUNT, Map.of("processing_type", List.of("active_workers")),
                        PRODUCTIVITY, Map.of("ability_level", List.of("1"))
                        ))
                .build();

        final GetHeadcountInput headcountEntityInput =
                GetHeadcountInput.builder()
                        .warehouseId(WAREHOUSE_ID)
                        .workflow(FBM_WMS_OUTBOUND)
                        .source(FORECAST)
                        .dateFrom(A_DATE_UTC)
                        .dateTo(A_DATE_UTC.plusHours(12))
                        .processName(List.of(PICKING, PACKING))
                        .processingType(Set.of(ACTIVE_WORKERS))
                        .entityType(HEADCOUNT)
                        .build();

        final GetProductivityInput productivityEntityInput =
                GetProductivityInput.builder()
                        .warehouseId(WAREHOUSE_ID)
                        .workflow(FBM_WMS_OUTBOUND)
                        .source(FORECAST)
                        .dateFrom(A_DATE_UTC)
                        .dateTo(A_DATE_UTC.plusHours(12))
                        .processName(List.of(PICKING, PACKING))
                        .abilityLevel(Set.of(1))
                        .entityType(PRODUCTIVITY)
                        .build();

        when(getEntitiesStrategy.getBy(HEADCOUNT))
                .thenReturn(Optional.of(getHeadcountEntityUseCase));
        when(getHeadcountEntityUseCase.execute(eq(headcountEntityInput)))
                .thenReturn(mockHeadcountEntityOutput());

        when(getEntitiesStrategy.getBy(PRODUCTIVITY))
                .thenReturn(Optional.of(getProductivityEntityUseCase));
        when(getProductivityEntityUseCase.execute(eq(productivityEntityInput)))
                .thenReturn(mockProductivityEntityOutput());

        when(getEntitiesStrategy.getBy(REMAINING_PROCESSING))
                .thenReturn(Optional.of(getRemainingProcessingUseCase));
        when(getRemainingProcessingUseCase.execute(any(GetEntityInput.class)))
                .thenReturn(mockGetRemainingProcessingOutput());

        when(getEntitiesStrategy.getBy(THROUGHPUT))
                .thenReturn(Optional.of(getThroughputUseCase));
        when(getThroughputUseCase.execute(any(GetEntityInput.class)))
                .thenReturn(mockThroughputEntityOutput());

        // WHEN
        final Map<EntityType, Object> results = searchEntitiesUseCase.execute(input);

        // THEN
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(4, results.size());
        assertTrue(results.keySet().containsAll(Arrays.asList(EntityType.values().clone())));
    }
}
