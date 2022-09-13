package com.mercadolibre.planning.model.api.usecase;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static com.mercadolibre.planning.model.api.util.ProjectionTestsUtils.A_FIXED_DATE;
import static com.mercadolibre.planning.model.api.util.ProjectionTestsUtils.mockPlanningDistributionOutputs;
import static com.mercadolibre.planning.model.api.util.ProjectionTestsUtils.mockThroughputEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.BacklogProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.PackingWallBacklogProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.ProcessParams;
import com.mercadolibre.planning.model.api.web.controller.projection.request.CurrentBacklog;
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
public class PackingWallBacklogProjectionUseCaseTest {

  @InjectMocks
  private PackingWallBacklogProjectionUseCase packingWallBacklogProjectionUseCase;

  @Test
  public void createPackingProcessParams() {
    // GIVEN
    final BacklogProjectionInput input = BacklogProjectionInput.builder()
        .processNames(List.of(WAVING, PICKING, PACKING_WALL))
        .throughputs(mockThroughputs())
        .currentBacklogs(List.of(
            new CurrentBacklog(WAVING, 0),
            new CurrentBacklog(PICKING, 3000),
            new CurrentBacklog(PACKING_WALL, 1110)))
        .dateFrom(A_FIXED_DATE.minusMinutes(15))
        .dateTo(A_FIXED_DATE.plusHours(4))
        .planningUnits(mockPlanningDistributionOutputs())
        .packingWallRatios(Map.of(A_FIXED_DATE, 0.00))
        .build();

    // WHEN
    final ProcessParams processParams = packingWallBacklogProjectionUseCase.execute(null, input);

    // THEN
    assertEquals(PACKING_WALL, processParams.getProcessName());
    assertEquals(1110, processParams.getCurrentBacklog());
    assertNull(processParams.getProcessedUnitsByDate());

    assertEquals(650, processParams.getCapacityByDate().get(A_FIXED_DATE.minusHours(1)));
    assertEquals(550, processParams.getCapacityByDate().get(A_FIXED_DATE));
    assertEquals(700, processParams.getCapacityByDate().get(A_FIXED_DATE.plusHours(1)));
    assertEquals(700, processParams.getCapacityByDate().get(A_FIXED_DATE.plusHours(2)));
    assertEquals(50, processParams.getCapacityByDate().get(A_FIXED_DATE.plusHours(3)));
  }

  @ParameterizedTest
  @MethodSource("getSupportedProcesses")
  public void supportsPackingProcess(final ProcessName processName, final boolean isSupported) {
    // WHEN
    final boolean result = packingWallBacklogProjectionUseCase.supportsProcessName(processName);

    // THEN
    assertEquals(isSupported, result);
  }

  private static Stream<Arguments> getSupportedProcesses() {
    return Stream.of(
        Arguments.of(WAVING, false),
        Arguments.of(PICKING, false),
        Arguments.of(PACKING_WALL, true)
    );
  }

  private static List<EntityOutput> mockThroughputs() {
    return List.of(
        mockThroughputEntity(A_FIXED_DATE.minusHours(1), PICKING, 850),
        mockThroughputEntity(A_FIXED_DATE.minusHours(1), PACKING_WALL, 650),
        mockThroughputEntity(A_FIXED_DATE, PICKING, 800),
        mockThroughputEntity(A_FIXED_DATE, PACKING_WALL, 550),
        mockThroughputEntity(A_FIXED_DATE.plusHours(1), PICKING, 600),
        mockThroughputEntity(A_FIXED_DATE.plusHours(1), PACKING_WALL, 700),
        mockThroughputEntity(A_FIXED_DATE.plusHours(2), PICKING, 600),
        mockThroughputEntity(A_FIXED_DATE.plusHours(2), PACKING_WALL, 700),
        mockThroughputEntity(A_FIXED_DATE.plusHours(3), PICKING, 0),
        mockThroughputEntity(A_FIXED_DATE.plusHours(3), PACKING_WALL, 50),
        mockThroughputEntity(A_FIXED_DATE.plusHours(4), PICKING, 1000),
        mockThroughputEntity(A_FIXED_DATE.plusHours(4), PACKING_WALL, 910)
    );
  }
}
