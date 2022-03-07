package com.mercadolibre.planning.model.api.domain.entity;

import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Tests Workflow enum behaviour. */
public class WorkflowTest {

  private static Stream<Arguments> execute() {
    return Stream.of(
        arguments(FBM_WMS_INBOUND, 10, 11),
        arguments(FBM_WMS_OUTBOUND, 10, 9)
    );
  }

  /**
   * Tests executor service invocation.
   *
   * @param  workflow target workflow.
   * @param  param    executor parameters.
   * @param  expected expected output.
   */
  @ParameterizedTest
  @MethodSource("execute")
  public void testExecute(final Workflow workflow, final int param, final int expected) {
    // GIVEN
    final var service = new WorkflowService<Integer, Integer>() {

      @Override
      public Integer executeInbound(final Integer params) {
        return params + 1;
      }

      @Override
      public Integer executeOutbound(final Integer params) {
        return params - 1;
      }
    };

    // WHEN
    final int actual = workflow.execute(service, param);

    // THEN
    assertEquals(expected, actual);
  }

}
