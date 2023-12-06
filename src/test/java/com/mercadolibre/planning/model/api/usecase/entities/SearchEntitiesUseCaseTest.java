package com.mercadolibre.planning.model.api.usecase.entities;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.EFFECTIVE_WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockGetPerformedProcessingOutput;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockGetRemainingProcessingOutput;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockHeadcountEntityOutput;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockProductivityEntityOutput;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockSearchBacklogLowerLimitOutput;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockSearchBacklogUpperLimitOutput;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockThroughputEntityOutput;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.BACKLOG_LOWER_LIMIT;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.BACKLOG_UPPER_LIMIT;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.PERFORMED_PROCESSING;
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

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntitiesStrategy;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.SearchEntitiesUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.input.SearchEntitiesInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.search.SearchEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private GetThroughputUseCase getThroughputUseCase;

    @Mock
    private SearchEntityUseCase searchEntityUseCase;


    @Test
    @DisplayName("Search all entities")
    public void testSearchAllEntitiesOk() {
        // GIVEN
        final SearchEntitiesInput input = SearchEntitiesInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .entityTypes(List.of(HEADCOUNT, PRODUCTIVITY, REMAINING_PROCESSING, THROUGHPUT,
                        PERFORMED_PROCESSING, BACKLOG_LOWER_LIMIT, BACKLOG_UPPER_LIMIT))
                .source(FORECAST)
                .dateFrom(A_DATE_UTC)
                .dateTo(A_DATE_UTC.plusHours(12))
                .processName(List.of(PICKING, PACKING))
                .entityFilters(Map.of(
                        HEADCOUNT, Map.of("processing_type", List.of("effective_workers")),
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
                        .processingType(Set.of(EFFECTIVE_WORKERS))
                        .entityType(HEADCOUNT)
                        .processPaths(List.of(ProcessPath.GLOBAL))
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
                        .processPaths(List.of(ProcessPath.GLOBAL))
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
                .thenReturn(Optional.of(searchEntityUseCase));
        when(searchEntityUseCase.execute(any(GetEntityInput.class)))
                .thenReturn(mockGetRemainingProcessingOutput());

        when(getEntitiesStrategy.getBy(PERFORMED_PROCESSING))
                .thenReturn(Optional.of(searchEntityUseCase));
        when(searchEntityUseCase.execute(getMockInput(PERFORMED_PROCESSING)))
                .thenReturn(mockGetPerformedProcessingOutput());

        when(getEntitiesStrategy.getBy(THROUGHPUT))
                .thenReturn(Optional.of(getThroughputUseCase));
        when(getThroughputUseCase.execute(any(GetEntityInput.class)))
                .thenReturn(mockThroughputEntityOutput());

        when(getEntitiesStrategy.getBy(BACKLOG_LOWER_LIMIT))
                .thenReturn(Optional.of(searchEntityUseCase));
        when(searchEntityUseCase.execute(getMockInput(BACKLOG_LOWER_LIMIT)))
                .thenReturn(mockSearchBacklogLowerLimitOutput());

        when(getEntitiesStrategy.getBy(BACKLOG_UPPER_LIMIT))
                .thenReturn(Optional.of(searchEntityUseCase));
        when(searchEntityUseCase.execute(getMockInput(BACKLOG_UPPER_LIMIT)))
                .thenReturn(mockSearchBacklogUpperLimitOutput());

        final Map<EntityType, Object> results = searchEntitiesUseCase.execute(input);

        // THEN
        final Set<EntityType> searchableEntityType = Set.of(
                HEADCOUNT,
                PRODUCTIVITY,
                THROUGHPUT,
                REMAINING_PROCESSING,
                PERFORMED_PROCESSING,
                BACKLOG_LOWER_LIMIT,
                BACKLOG_UPPER_LIMIT);

        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(7, results.size());
        assertTrue(results.keySet().containsAll(searchableEntityType));
    }

    private GetEntityInput getMockInput(final EntityType entityType) {
        return GetEntityInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .processPaths(List.of(ProcessPath.GLOBAL))
                .source(FORECAST)
                .dateFrom(A_DATE_UTC)
                .dateTo(A_DATE_UTC.plusHours(12))
                .processName(List.of(PICKING, PACKING))
                .entityType(entityType)
                .build();
    }
}
