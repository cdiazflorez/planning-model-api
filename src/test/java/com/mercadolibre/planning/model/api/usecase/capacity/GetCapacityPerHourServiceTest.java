package com.mercadolibre.planning.model.api.usecase.capacity;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.CapacityInput;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.CapacityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.GetCapacityPerHourService;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests {@link GetCapacityPerHourService}. */
@ExtendWith(MockitoExtension.class)
public class GetCapacityPerHourServiceTest {

  @InjectMocks
  private GetCapacityPerHourService useCase;

  private static Stream<Arguments> getMaxCapacityPerHourByProcess() {
    return Stream.of(
        Arguments.of(
            FBM_WMS_OUTBOUND,
            List.of(
                CapacityInput.builder()
                    .processName(PICKING)
                    .date(A_DATE_UTC)
                    .metricUnit(UNITS_PER_HOUR)
                    .value(60)
                    .build(),
                CapacityInput.builder()
                    .processName(PACKING)
                    .date(A_DATE_UTC)
                    .metricUnit(UNITS_PER_HOUR)
                    .value(45)
                    .build(),
                CapacityInput.builder()
                    .processName(PACKING_WALL)
                    .date(A_DATE_UTC)
                    .metricUnit(UNITS_PER_HOUR)
                    .value(49)
                    .build(),
                CapacityInput.builder()
                    .processName(PICKING)
                    .date(A_DATE_UTC.plusHours(1))
                    .metricUnit(UNITS_PER_HOUR)
                    .value(30)
                    .build(),
                CapacityInput.builder()
                    .processName(PACKING)
                    .date(A_DATE_UTC.plusHours(1))
                    .metricUnit(UNITS_PER_HOUR)
                    .value(8)
                    .build(),
                CapacityInput.builder()
                    .processName(PACKING_WALL)
                    .date(A_DATE_UTC.plusHours(1))
                    .metricUnit(UNITS_PER_HOUR)
                    .value(19)
                    .build()
            ), List.of(60, 30)),
        Arguments.of(
            FBM_WMS_OUTBOUND,
            List.of(
                CapacityInput.builder()
                    .processName(PICKING)
                    .date(A_DATE_UTC)
                    .metricUnit(UNITS_PER_HOUR)
                    .value(60)
                    .build(),
                CapacityInput.builder()
                    .processName(PACKING)
                    .date(A_DATE_UTC)
                    .metricUnit(UNITS_PER_HOUR)
                    .value(30)
                    .build(),
                CapacityInput.builder()
                    .processName(PACKING_WALL)
                    .date(A_DATE_UTC)
                    .metricUnit(UNITS_PER_HOUR)
                    .value(29)
                    .build(),
                CapacityInput.builder()
                    .processName(PICKING)
                    .date(A_DATE_UTC.plusHours(1))
                    .metricUnit(UNITS_PER_HOUR)
                    .value(13)
                    .build(),
                CapacityInput.builder()
                    .processName(PACKING)
                    .date(A_DATE_UTC.plusHours(1))
                    .metricUnit(UNITS_PER_HOUR)
                    .value(24)
                    .build()
            ), List.of(60, 13)),
        Arguments.of(
            FBM_WMS_INBOUND,
            List.of(
                CapacityInput.builder()
                    .processName(PUT_AWAY)
                    .date(A_DATE_UTC)
                    .metricUnit(UNITS_PER_HOUR)
                    .value(46)
                    .build(),
                CapacityInput.builder()
                    .processName(PUT_AWAY)
                    .date(A_DATE_UTC.plusHours(1))
                    .metricUnit(UNITS_PER_HOUR)
                    .value(35)
                    .build()
            ), List.of(46, 35)),
        Arguments.of(
            FBM_WMS_OUTBOUND,
            List.of(
                CapacityInput.builder()
                    .processName(PACKING)
                    .date(A_DATE_UTC)
                    .metricUnit(UNITS_PER_HOUR)
                    .value(30)
                    .build(),
                CapacityInput.builder()
                    .processName(PACKING_WALL)
                    .date(A_DATE_UTC)
                    .metricUnit(UNITS_PER_HOUR)
                    .value(29)
                    .build(),
                CapacityInput.builder()
                    .processName(PACKING)
                    .date(A_DATE_UTC.plusHours(1))
                    .metricUnit(UNITS_PER_HOUR)
                    .value(24)
                    .build()
            ), List.of(59, 24))
    );
  }

  /**
   * Tests the capacity calculation algorithms.
   *
   * @param  workflow       input to the execute method.
   * @param  capacityInputs input to the execute method.
   * @param  capacityValues expected output values.
   */
  @ParameterizedTest
  @MethodSource("getMaxCapacityPerHourByProcess")
  public void testCapacityPerHour(final Workflow workflow,
                                  final List<CapacityInput> capacityInputs,
                                  final List<Integer> capacityValues) {

    // WHEN
    final List<CapacityOutput> output = useCase.execute(workflow, capacityInputs);

    // THEN
    final List<CapacityOutput> expected = mockCapacityOutput(capacityValues);
    assertEquals(expected.size(), output.size());
    assertEquals(output, expected);
  }

  private List<CapacityOutput> mockCapacityOutput(final List<Integer> expectedValue) {
    final List<CapacityOutput> capacityOutput = new ArrayList<>();
    IntStream.range(0, expectedValue.size())
        .forEach(index -> capacityOutput.add(
            new CapacityOutput(A_DATE_UTC.withFixedOffsetZone().plusHours(index),
                UNITS_PER_HOUR, expectedValue.get(index))
        ));
    return capacityOutput;
  }
}
