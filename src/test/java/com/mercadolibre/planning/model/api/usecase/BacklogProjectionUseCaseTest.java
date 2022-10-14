package com.mercadolibre.planning.model.api.usecase;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static com.mercadolibre.planning.model.api.util.ProjectionTestsUtils.A_FIXED_DATE;
import static com.mercadolibre.planning.model.api.util.ProjectionTestsUtils.mockBacklogProjectionInput;
import static com.mercadolibre.planning.model.api.util.ProjectionTestsUtils.mockPlanningSalesByDate;
import static com.mercadolibre.planning.model.api.util.ProjectionTestsUtils.mockThroughputs;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.BacklogProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.BacklogProjectionStrategy;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.BatchSorterBacklogProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.CalculateBacklogProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.PackingRegularBacklogProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.PickingBacklogProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.ProcessParams;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.WavingBacklogProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.BacklogProjection;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.BacklogProjectionOutputValue;
import com.mercadolibre.planning.model.api.web.controller.projection.request.CurrentBacklog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@ExtendWith(MockitoExtension.class)
public class BacklogProjectionUseCaseTest {

    @InjectMocks
    private CalculateBacklogProjectionUseCase calculateBacklogProjection;

    @Mock
    private BacklogProjectionStrategy backlogProjectionStrategy;

    @Mock
    private WavingBacklogProjectionUseCase wavingBacklogProjection;

    @Mock
    private PickingBacklogProjectionUseCase pickingBacklogProjection;

    @Mock
    private PackingRegularBacklogProjectionUseCase packingBacklogProjection;

    @Mock
    private BatchSorterBacklogProjectionUseCase batchSortedBacklogProjection;

    @Test
    @DisplayName("Project all backlogs")
    public void testProjectAllBacklogs() {
        // GIVEN
        final List<ProcessName> processNames = new ArrayList<>();
        processNames.add(WAVING);
        processNames.add(PICKING);
        processNames.add(PACKING);
        processNames.add(BATCH_SORTER);

        final BacklogProjectionInput input = mockBacklogProjectionInput(
                processNames,
                List.of(new CurrentBacklog(WAVING, 0),
                        new CurrentBacklog(PICKING, 3000),
                        new CurrentBacklog(PACKING, 1110),
                        new CurrentBacklog(BATCH_SORTER, 1800)),
                A_FIXED_DATE.plusHours(4)
        );

        mockWavingBacklogProjection(input);
        mockPickingBacklogProjection(input);
        mockPackingBacklogProjection(input);
        mockBatchSortedProjection(input);

        // WHEN
        final List<BacklogProjection> results = calculateBacklogProjection.execute(input);

        // THEN
        assertEquals(4, results.size());

        assertBacklogOutputs(List.of(50L, 0L, 0L, 0L, 1400L), WAVING, results.get(0));
        assertBacklogOutputs(List.of(2788L, 1988L, 1738L, 1538L, 1538L), PICKING, results.get(1));
        assertBacklogOutputs(List.of(948L, 610L, 710L, 610L, 1160L), PACKING, results.get(2));
        assertBacklogOutputs(List.of(1638L, 1194L, 994L, 669L, 544L), BATCH_SORTER, results.get(3));
    }

    private void assertBacklogOutputs(final List<Long> wantedQuantities,
                                      final ProcessName wantedProcess,
                                      final BacklogProjection backlogOutputs) {

        assertEquals(wantedProcess, backlogOutputs.getProcessName());

        final List<BacklogProjectionOutputValue> values = backlogOutputs.getValues();
        for (final BacklogProjectionOutputValue value : values) {
            final int valueIndex = values.indexOf(value);
            assertEquals(A_FIXED_DATE.plusHours(valueIndex), value.getDate());
            assertEquals((long) wantedQuantities.get(valueIndex), value.getQuantity());
        }
    }

    private void mockWavingBacklogProjection(final BacklogProjectionInput input) {
        when(backlogProjectionStrategy.getBy(WAVING))
                .thenReturn(Optional.of(wavingBacklogProjection));

        when(wavingBacklogProjection.execute(WAVING, input))
                .thenReturn(ProcessParams.builder()
                        .processName(WAVING)
                        .currentBacklog(0)
                        .capacityByDate(mockThroughputs().stream().collect(toMap(
                                EntityOutput::getDate,
                                EntityOutput::getValue,
                                Math::min)))
                        .planningUnitsByDate(mockPlanningSalesByDate())
                        .processedUnitsByDate(null)
                        .build());
    }

    private void mockPickingBacklogProjection(final BacklogProjectionInput input) {
        when(backlogProjectionStrategy.getBy(PICKING))
                .thenReturn(Optional.of(pickingBacklogProjection));

        when(pickingBacklogProjection.execute(PICKING, input))
                .thenReturn(ProcessParams.builder()
                        .processName(PICKING)
                        .currentBacklog(3000)
                        .capacityByDate(mockThroughputs().stream()
                                .filter(e -> e.getProcessName() == PICKING)
                                .collect(toMap(EntityOutput::getDate, EntityOutput::getValue)))
                        .planningUnitsByDate(input.getThroughputs().stream()
                                .collect(toMap(EntityOutput::getDate, EntityOutput::getValue, Math::min)))
                        .processedUnitsByDate(Map.of(
                                A_FIXED_DATE, 50L,
                                A_FIXED_DATE.plusHours(1), 0L,
                                A_FIXED_DATE.plusHours(2), 0L,
                                A_FIXED_DATE.plusHours(3), 0L,
                                A_FIXED_DATE.plusHours(4), 1400L))
                        .build());
    }

    private void mockPackingBacklogProjection(final BacklogProjectionInput input) {
        when(backlogProjectionStrategy.getBy(PACKING))
                .thenReturn(Optional.of(packingBacklogProjection));

        when(packingBacklogProjection.execute(PACKING, input))
                .thenReturn(ProcessParams.builder()
                        .processName(PACKING)
                        .currentBacklog(1110)
                        .capacityByDate(mockThroughputs().stream()
                                .filter(e -> e.getProcessName() == PACKING)
                                .collect(toMap(EntityOutput::getDate, EntityOutput::getValue)))
                        .planningUnitsByDate(input.getThroughputs().stream()
                                .filter(e -> e.getProcessName() == PICKING)
                                .collect(toMap(EntityOutput::getDate, EntityOutput::getValue)))
                        .build());
    }

    private void mockBatchSortedProjection(final BacklogProjectionInput input) {
        when(backlogProjectionStrategy.getBy(BATCH_SORTER))
            .thenReturn(Optional.of(batchSortedBacklogProjection));

        when(batchSortedBacklogProjection.execute(BATCH_SORTER, input))
            .thenReturn(ProcessParams.builder()
                .processName(BATCH_SORTER)
                .currentBacklog(1800)
                .capacityByDate(mockThroughputs().stream()
                    .filter(e -> e.getProcessName() == BATCH_SORTER)
                    .collect(toMap(EntityOutput::getDate, EntityOutput::getValue)))
                .planningUnitsByDate(input.getThroughputs().stream()
                    .filter(e -> e.getProcessName() == BATCH_SORTER)
                    .collect(toMap(EntityOutput::getDate, EntityOutput::getValue)))
                .ratiosByDate(Map.of(
                    A_FIXED_DATE, 0.5,
                    A_FIXED_DATE.plusHours(1), 0.5,
                    A_FIXED_DATE.plusHours(2), 0.625,
                    A_FIXED_DATE.plusHours(3), 0.375,
                    A_FIXED_DATE.plusHours(4), 0.715))
                .build());
    }
}
