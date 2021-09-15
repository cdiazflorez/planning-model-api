package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.BacklogProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.PackingBacklogProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.ProcessParams;
import com.mercadolibre.planning.model.api.exception.BadRequestException;
import com.mercadolibre.planning.model.api.web.controller.projection.request.CurrentBacklog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
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

@ExtendWith(MockitoExtension.class)
public class PackingBacklogProjectionUseCaseTest {

    @InjectMocks
    private PackingBacklogProjectionUseCase packingBacklogProjection;

    @Test
    public void createPackingProcessParams() {
        // GIVEN
        final BacklogProjectionInput input = BacklogProjectionInput.builder()
                .processNames(List.of(WAVING, PICKING, PACKING, PACKING_WALL))
                .throughputs(mockThroughputs())
                .currentBacklogs(List.of(
                        new CurrentBacklog(WAVING, 0),
                        new CurrentBacklog(PICKING, 3000),
                        new CurrentBacklog(PACKING, 1110)))
                .dateFrom(A_FIXED_DATE.minusMinutes(15))
                .dateTo(A_FIXED_DATE.plusHours(4))
                .planningUnits(mockPlanningDistributionOutputs())
                .build();

        // WHEN
        final ProcessParams processParams = packingBacklogProjection.execute(input);

        // THEN
        assertEquals(PACKING, processParams.getProcessName());
        assertEquals(1110, processParams.getCurrentBacklog());
        assertNull(processParams.getPreviousBacklogsByDate());

        assertEquals(650, (int)processParams.getCapacityByDate().get(A_FIXED_DATE.minusHours(1)));
        assertEquals(550, (int)processParams.getCapacityByDate().get(A_FIXED_DATE));
        assertEquals(700, (int)processParams.getCapacityByDate().get(A_FIXED_DATE.plusHours(1)));
        assertEquals(700, (int)processParams.getCapacityByDate().get(A_FIXED_DATE.plusHours(2)));
        assertEquals(50, (int)processParams.getCapacityByDate().get(A_FIXED_DATE.plusHours(3)));
        assertEquals(1500, (int)processParams.getCapacityByDate().get(A_FIXED_DATE.plusHours(4)));

        final List<EntityOutput> pickingCapacity = mockThroughputs().stream()
                .filter(e -> e.getProcessName() == PICKING).collect(toList());
        assertPlanningUnits(processParams.getPlanningUnitsByDate(), pickingCapacity);
    }

    @Test
    public void noCurrentBacklogThrowException() {
        // GIVEN
        final BacklogProjectionInput input = mockBacklogProjectionInput(
                List.of(WAVING, PICKING, PACKING), emptyList(), A_FIXED_DATE.plusHours(4));

        // WHEN
        final BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> packingBacklogProjection.execute(input));

        // THEN
        assertEquals("No current backlog for Packing", exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("getSupportedProcesses")
    public void supportsPackingProcess(final ProcessName processName, final boolean isSupported) {
        // WHEN
        final boolean result = packingBacklogProjection.supportsProcessName(processName);

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
                Arguments.of(PACKING, true)
        );
    }

    private static List<EntityOutput> mockThroughputs() {
        return List.of(
                mockThroughputEntity(A_FIXED_DATE.minusHours(1), PICKING, 850),
                mockThroughputEntity(A_FIXED_DATE.minusHours(1), PACKING, 650),
                mockThroughputEntity(A_FIXED_DATE, PICKING, 800),
                mockThroughputEntity(A_FIXED_DATE, PACKING, 550),
                mockThroughputEntity(A_FIXED_DATE.plusHours(1), PICKING, 600),
                mockThroughputEntity(A_FIXED_DATE.plusHours(1), PACKING, 700),
                mockThroughputEntity(A_FIXED_DATE.plusHours(2), PICKING, 600),
                mockThroughputEntity(A_FIXED_DATE.plusHours(2), PACKING, 700),
                mockThroughputEntity(A_FIXED_DATE.plusHours(3), PICKING, 0),
                mockThroughputEntity(A_FIXED_DATE.plusHours(3), PACKING, 50),
                mockThroughputEntity(A_FIXED_DATE.plusHours(4), PICKING, 1000),
                mockThroughputEntity(A_FIXED_DATE.plusHours(4), PACKING, 910),
                mockThroughputEntity(A_FIXED_DATE.plusHours(4), PACKING_WALL, 590)
        );
    }
}
