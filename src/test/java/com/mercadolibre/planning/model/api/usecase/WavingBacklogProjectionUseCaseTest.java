package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.output.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.BacklogProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.ProcessParams;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.WavingBacklogProjectionUseCase;
import com.mercadolibre.planning.model.api.exception.BadRequestException;
import com.mercadolibre.planning.model.api.web.controller.request.projection.CurrentBacklog;
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
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static com.mercadolibre.planning.model.api.util.DateUtils.ignoreMinutes;
import static com.mercadolibre.planning.model.api.util.ProjectionTestsUtils.A_FIXED_DATE;
import static com.mercadolibre.planning.model.api.util.ProjectionTestsUtils.assertCapacityByDate;
import static com.mercadolibre.planning.model.api.util.ProjectionTestsUtils.getMinCapacity;
import static com.mercadolibre.planning.model.api.util.ProjectionTestsUtils.mockBacklogProjectionInput;
import static com.mercadolibre.planning.model.api.util.ProjectionTestsUtils.mockPlanningDistributionOutputs;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class WavingBacklogProjectionUseCaseTest {

    @InjectMocks
    private WavingBacklogProjectionUseCase wavingBacklogProjection;

    @Test
    public void createWavingProcessParams() {
        // GIVEN
        final BacklogProjectionInput input = mockBacklogProjectionInput(
                List.of(WAVING, PICKING, PACKING),
                List.of(new CurrentBacklog(WAVING, 0),
                        new CurrentBacklog(PICKING, 3000),
                        new CurrentBacklog(PACKING, 1110)),
                A_FIXED_DATE.plusHours(4));

        // WHEN
        final ProcessParams processParams = wavingBacklogProjection.execute(input);

        // THEN
        assertEquals(WAVING, processParams.getProcessName());
        assertEquals(0, processParams.getCurrentBacklog());
        assertNull(processParams.getPreviousBacklogsByDate());
        assertCapacityByDate(processParams.getCapacityByDate(), getMinCapacity());
        assertPlanningUnits(processParams.getPlanningUnitsByDate());
    }

    @Test
    public void noCurrentBacklogThrowException() {
        // GIVEN
        final BacklogProjectionInput input = mockBacklogProjectionInput(
                List.of(WAVING, PICKING, PACKING), emptyList(), A_FIXED_DATE.plusHours(4));

        // WHEN
        final BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> wavingBacklogProjection.execute(input));

        // THEN
        assertEquals("No current backlog for Waving", exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("getSupportedProcesses")
    public void supportsWavingProcess(final ProcessName processName, final boolean isSupported) {
        // WHEN
        final boolean result = wavingBacklogProjection.supportsProcessName(processName);

        // THEN
        assertEquals(isSupported, result);
    }

    private void assertPlanningUnits(final Map<ZonedDateTime, Long> planningUnitsByDate) {
        final Map<ZonedDateTime, Long> wantedSales = mockPlanningDistributionOutputs().stream()
                .collect(toMap(o -> ignoreMinutes(o.getDateIn()),
                        GetPlanningDistributionOutput::getTotal,
                        Long::sum));

        assertEquals(wantedSales, planningUnitsByDate);
    }

    private static Stream<Arguments> getSupportedProcesses() {
        return Stream.of(
                Arguments.of(WAVING, true),
                Arguments.of(PICKING, false),
                Arguments.of(PACKING, false)
        );
    }
}
