package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.BacklogProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.PackingBacklogProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.PickingBacklogProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.ProcessParams;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.WavingBacklogProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.BacklogProjectionStrategy;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.CalculateBacklogProjectionUseCase;
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
    private PackingBacklogProjectionUseCase packingBacklogProjection;

    @Test
    @DisplayName("Project all backlogs")
    public void testProjectAllBacklogs() {
        // GIVEN
        final List<ProcessName> processNames = new ArrayList<>();
        processNames.add(WAVING);
        processNames.add(PICKING);
        processNames.add(PACKING);

        final BacklogProjectionInput input = mockBacklogProjectionInput(
                processNames,
                List.of(new CurrentBacklog(WAVING, 0),
                        new CurrentBacklog(PICKING, 3000),
                        new CurrentBacklog(PACKING, 1110)),
                A_FIXED_DATE.plusHours(4)
        );

        mockWavingBacklogProjection(input);
        mockPickingBacklogProjection(input);
        mockPackingBacklogProjection(input);

        // WHEN
        final List<BacklogProjection> results = calculateBacklogProjection.execute(input);

        // THEN
        assertEquals(3, results.size());

        assertBacklogOutputs(List.of(50L, 0L, 0L, 0L, 1400L), WAVING, results.get(0));
        assertBacklogOutputs(List.of(2788L, 2038L, 1438L, 838L, 838L), PICKING,  results.get(1));
        assertBacklogOutputs(List.of(1160L, 1410L, 1310L, 1210L, 1160L), PACKING, results.get(2));
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
                        .previousBacklogsByDate(null)
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
                        .previousBacklogsByDate(Map.of(
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
                        .previousBacklogsByDate(null)
                        .build());
    }
}
