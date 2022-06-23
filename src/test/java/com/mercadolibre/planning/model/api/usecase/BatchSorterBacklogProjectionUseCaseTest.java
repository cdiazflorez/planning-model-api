package com.mercadolibre.planning.model.api.usecase;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static com.mercadolibre.planning.model.api.util.ProjectionTestsUtils.A_FIXED_DATE;
import static com.mercadolibre.planning.model.api.util.ProjectionTestsUtils.mockBacklogProjectionInput;
import static com.mercadolibre.planning.model.api.util.ProjectionTestsUtils.mockPlanningDistributionOutputs;
import static com.mercadolibre.planning.model.api.util.ProjectionTestsUtils.mockThroughputEntity;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.BacklogProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.BatchSorterBacklogProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.ProcessParams;
import com.mercadolibre.planning.model.api.exception.BadRequestException;
import com.mercadolibre.planning.model.api.web.controller.projection.request.CurrentBacklog;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BatchSorterBacklogProjectionUseCaseTest {

    @InjectMocks
    private BatchSorterBacklogProjectionUseCase consolidationBacklogProjection;

    @Test
    public void createPackingProcessParams() {
        // GIVEN
        final BacklogProjectionInput input = BacklogProjectionInput.builder()
                .processNames(List.of(WAVING, PICKING, BATCH_SORTER))
                .throughputs(mockThroughputs())
                .currentBacklogs(List.of(
                        new CurrentBacklog(WAVING, 0),
                        new CurrentBacklog(PICKING, 3000),
                        new CurrentBacklog(BATCH_SORTER, 1110)))
                .dateFrom(A_FIXED_DATE.minusMinutes(15))
                .dateTo(A_FIXED_DATE.plusHours(4))
                .planningUnits(mockPlanningDistributionOutputs())
                .ratioPackingRegular(0.0)
                .build();

        // WHEN
        final ProcessParams processParams = consolidationBacklogProjection.execute(null, input);

        // THEN
        assertEquals(BATCH_SORTER, processParams.getProcessName());
        assertEquals(1110, processParams.getCurrentBacklog());
        assertNull(processParams.getPreviousBacklogsByDate());

        assertEquals(650, processParams.getCapacityByDate().get(A_FIXED_DATE.minusHours(1)));
        assertEquals(550, processParams.getCapacityByDate().get(A_FIXED_DATE));
        assertEquals(700, processParams.getCapacityByDate().get(A_FIXED_DATE.plusHours(1)));
        assertEquals(700, processParams.getCapacityByDate().get(A_FIXED_DATE.plusHours(2)));
        assertEquals(50, processParams.getCapacityByDate().get(A_FIXED_DATE.plusHours(3)));

        final List<EntityOutput> pickingCapacity = mockThroughputs().stream()
                .filter(e -> e.getProcessName() == PICKING).collect(toList());
        assertPlanningUnits(processParams.getPlanningUnitsByDate(), pickingCapacity);
    }

    @Test
    public void noCurrentBacklogThrowException() {
        // GIVEN
        final BacklogProjectionInput input = mockBacklogProjectionInput(
                List.of(WAVING, PICKING, BATCH_SORTER), emptyList(), A_FIXED_DATE.plusHours(4));

        // WHEN
        final BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> consolidationBacklogProjection.execute(null, input));

        // THEN
        assertEquals("No current backlog for Consolidation", exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("getSupportedProcesses")
    public void supportsPackingProcess(final ProcessName processName, final boolean isSupported) {
        // WHEN
        final boolean result = consolidationBacklogProjection.supportsProcessName(processName);

        // THEN
        assertEquals(isSupported, result);
    }

    private void assertPlanningUnits(final Map<ZonedDateTime, Long> planningUnitsByDate,
                                     final List<EntityOutput> pickingCapacity) {
        for (final EntityOutput capacity : pickingCapacity) {
            assertTrue(planningUnitsByDate.containsKey(capacity.getDate()));
            assertEquals(capacity.getValue(), (long) planningUnitsByDate.get(capacity.getDate()));
        }
    }

    private static Stream<Arguments> getSupportedProcesses() {
        return Stream.of(
                Arguments.of(WAVING, false),
                Arguments.of(PICKING, false),
                Arguments.of(BATCH_SORTER, true)
        );
    }

    private static List<EntityOutput> mockThroughputs() {
        return List.of(
                mockThroughputEntity(A_FIXED_DATE.minusHours(1), PICKING, 850),
                mockThroughputEntity(A_FIXED_DATE.minusHours(1), BATCH_SORTER, 650),
                mockThroughputEntity(A_FIXED_DATE, PICKING, 800),
                mockThroughputEntity(A_FIXED_DATE, BATCH_SORTER, 550),
                mockThroughputEntity(A_FIXED_DATE.plusHours(1), PICKING, 600),
                mockThroughputEntity(A_FIXED_DATE.plusHours(1), BATCH_SORTER, 700),
                mockThroughputEntity(A_FIXED_DATE.plusHours(2), PICKING, 600),
                mockThroughputEntity(A_FIXED_DATE.plusHours(2), BATCH_SORTER, 700),
                mockThroughputEntity(A_FIXED_DATE.plusHours(3), PICKING, 0),
                mockThroughputEntity(A_FIXED_DATE.plusHours(3), BATCH_SORTER, 50),
                mockThroughputEntity(A_FIXED_DATE.plusHours(4), PICKING, 1000),
                mockThroughputEntity(A_FIXED_DATE.plusHours(4), BATCH_SORTER, 910)
        );
    }
}
