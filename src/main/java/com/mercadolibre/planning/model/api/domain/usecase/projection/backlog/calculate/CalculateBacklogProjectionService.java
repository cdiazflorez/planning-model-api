package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate;

import static com.mercadolibre.planning.model.api.util.DateUtils.instantRange;

import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.ProjectionResult;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Backlog projection service
 *
 * <p>
 *   Encapsulates the core algorithm and interfaces.
 * </p>
 */
public final class CalculateBacklogProjectionService {

  private CalculateBacklogProjectionService() {
  }

  /**
   * Backlog projection core.
   *
   * <p>
   * For each instant between dateFrom and dateTo will produce a partition of the backlog
   * according to the accumulated units and the available tph.
   * The backlog will be split according to the BacklogHelper consume method between processed units and the carry over.
   * </p>
   *
   * @param dateFrom        initial projection date.
   * @param dateTo          last projection date.
   * @param incomingBacklog upstream backlog.
   * @param initialBacklog  current process backlog.
   * @param throughput      tph representation.
   * @param helper          backlog type helper, consume and merge operations will be delegated to this object.
   * @param <T>             backlog type.
   * @return backlog projection results for each instant.
   */
  public static <T extends Backlog> List<ProjectionResult<T>> backlogProjectionBySla(final Instant dateFrom, final Instant dateTo,
                                                                                     final IncomingBacklog<T> incomingBacklog,
                                                                                     final T initialBacklog, final Throughput throughput,
                                                                                     final BacklogHelper<T> helper) {

    final List<ProjectionResult<T>> results = new ArrayList<>();
    final List<Instant> operatingHours = instantRange(dateFrom, dateTo, ChronoUnit.HOURS).collect(Collectors.toList());

    T carryOver = initialBacklog;
    for (Instant operatingHour : operatingHours) {
      final T upstream = incomingBacklog.get(operatingHour);

      final T current = helper.merge(carryOver, upstream);
      final ProcessedBacklog<T> afterProcessing = helper.consume(current, throughput.getAvailableQuantityFor(operatingHour));

      carryOver = afterProcessing.getCarryOver();
      results.add(new ProjectionResult<>(operatingHour, afterProcessing));
    }

    return results;
  }

  /**
   * Backlog marker interface.
   */
  public interface Backlog {
  }

  /**
   * Implements BacklogMerger and BacklogConsumer interfaces for a specific backlog type.
   *
   * @param <T> backlog type.
   */
  public interface BacklogHelper<T extends Backlog> extends BacklogMerger<T>, BacklogConsumer<T> {
  }

  /**
   * Backlog projection result at the end of an hour.
   *
   * <p>
   * The backlog projection results should be able to tell the state of the backlog at the end of the hour.
   * The state is represented by the processed and carried over backlog.
   * </p>
   *
   * @param <T> backlog type.
   */
  public interface ProcessedBacklog<T extends Backlog> {
    /**
     * Backlog that could not be processed.
     *
     * @return backlog
     */
    T getCarryOver();

    /**
     * Processed backlog.
     *
     * @return backlog
     */
    T getProcessed();
  }

  /**
   * Backlog Consumer Helper.
   *
   * @param <T> backlog type.
   */
  public interface BacklogConsumer<T extends Backlog> {

    /**
     * Processes n units of backlog returning a partition between the processed and unprocessed backlog.
     *
     * @param backlog current backlog.
     * @param units   how many units should be processed.
     * @return resulting partition.
     */
    ProcessedBacklog<T> consume(T backlog, int units);
  }

  /**
   * Backlog Merger Helper.
   *
   * @param <T> backlog type.
   */
  public interface BacklogMerger<T extends Backlog> {

    /**
     * Given two compatible backlog representations, return one representation of the accumulated backlog between the two inputs.
     *
     * @param one   a backlog representation.
     * @param other another backlog representation.
     * @return merged representation.
     */
    T merge(T one, T other);
  }

  /**
   * Represents the upstream or planned backlog.
   *
   * @param <T> backlog type.
   */
  public interface IncomingBacklog<T> {

    /**
     * Retrieve the incoming backlog for a specific point in time.
     *
     * @param operatingHour point in time.
     * @return backlog.
     */
    T get(Instant operatingHour);
  }

  /**
   * Represents the available tph.
   */
  public interface Throughput {

    /**
     * Retrieve the available tph from the operationHour up to the end of the hour.
     *
     * @param operatingHour target date.
     * @return available tph.
     */
    int getAvailableQuantityFor(Instant operatingHour);
  }

}
