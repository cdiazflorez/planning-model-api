package com.mercadolibre.planning.model.api.domain.entity;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PUT_AWAY;
import static java.lang.Math.min;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * A sequence of processes that to achieve a common goal.
 */
@AllArgsConstructor
public enum Workflow {
  FBM_WMS_INBOUND(Workflow::calculateInboundCapacity, Workflow::executeInbound, "inbound"),
  FBM_WMS_OUTBOUND(Workflow::calculateOutboundCapacity, Workflow::executeOutbound, "outbound-orders"),
  INBOUND_TRANSFER(null, Workflow::executeInbound, "inbound-transfer"),
  INBOUND(null, Workflow::executeInbound, "inbound");

  private static final Map<String, Workflow> LOOKUP = Arrays.stream(values()).collect(
      toMap(Workflow::toString, Function.identity())
  );

  @Getter
  private final Function<Map<ProcessName, Long>, Long> capacityCalculator;

  private final WorkflowExecutor executor;

  private final String alias;

  private static <T, V> V executeInbound(final WorkflowService<T, V> service, final T params) {
    return service.executeInbound(params);
  }

  private static <T, V> V executeOutbound(final WorkflowService<T, V> service, final T params) {
    return service.executeOutbound(params);
  }

  private static long calculateInboundCapacity(final Map<ProcessName, Long> capacities) {
    return capacities.getOrDefault(PUT_AWAY, 0L);
  }

  private static long calculateOutboundCapacity(final Map<ProcessName, Long> capacities) {
    return min(
        capacities.getOrDefault(PICKING, 0L),
        capacities.getOrDefault(PACKING, 0L)
            + capacities.getOrDefault(PACKING_WALL, 0L)
    );
  }

  /**
   * Deserialization of a String that represents a workflow.
   *
   * @param value the selected Workflow.
   * @return an Optional with the desired Workflow if the String matches a valid representation.
   */
  @JsonCreator
  public static Optional<Workflow> of(final String value) {
    return Optional.ofNullable(LOOKUP.get(value.toUpperCase(Locale.US).replace('-', '_')));
  }

  /**
   * Get name of workflow
   *
   * @return name of wokflow
   */
  public String getName() {
    return name().toLowerCase(Locale.ROOT);
  }

  /**
   * Get alias of workflow.
   *
   * @return alias of workflow
   */
  public String getAlias() {
    return alias;
  }

  /**
   * Serialization of a workflow to a String.
   *
   * @return a String that represents the Workflow.
   */
  @JsonValue
  public String toJson() {
    return toString().toLowerCase(Locale.US).replace('_', '-');
  }

  /**
   * Execution of a workflow service.
   *
   * @param service the selected service.
   * @param params  parameters to the service.
   * @param <T>     type of the service parameter.
   * @param <V>     return type of the service.
   * @return the service output.
   * @deprecated use strategy or enum approach
   */
  @Deprecated
  public <T, V> V execute(final WorkflowService<T, V> service, final T params) {
    return this.executor.execute(service, params);
  }

  /**
   * A service where the desired behaviour depends on the workflow.
   *
   * @deprecated use strategy or enum approach
   */
  @Deprecated
  public interface WorkflowExecutor {

    /**
     * Method to be executed.
     *
     * @param service the selected service.
     * @param params  parameters to the service.
     * @param <T>     type of the service parameter.
     * @param <V>     return type of the service.
     * @return the service output.
     * @deprecated use strategy or enum approach
     */
    @Deprecated
    <T, V> V execute(WorkflowService<T, V> service, T params);
  }
}
