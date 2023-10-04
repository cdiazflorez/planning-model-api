package com.mercadolibre.planning.model.api.projection.waverless.sla;

import com.mercadolibre.flow.projection.tools.services.entities.context.Backlog;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.function.LongUnaryOperator;
import java.util.stream.Collectors;
import lombok.Value;

@Value
public class BacklogAndForecastByDate implements Backlog {

  private static final BacklogAndForecastByDate EMPTY_BACKLOG =
      new BacklogAndForecastByDate(Collections.emptyMap());

  Map<Instant, ? extends BacklogAndForecast> backlogs;

  public static BacklogAndForecastByDate emptyBacklog() {
    return EMPTY_BACKLOG;
  }

  @Override
  public long total() {
    return backlogs.values().stream().mapToLong(Backlog::total).sum();
  }

  @Override
  public Backlog map(final LongUnaryOperator f) {
    return new BacklogAndForecastByDate(
        backlogs.entrySet().stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey, entry -> (BacklogAndForecast) entry.getValue().map(f))));
  }

  /** Defines the methods required by BacklogAndForecastByDate from any Backlog representation. */
  public interface BacklogAndForecast extends Backlog, Mergeable {
    long forecast();

    long backlog();
  }

  /**
   * Defines the methods that any Backlog implementation should implement in order to fusion more
   * than one representation into one.
   */
  public interface Mergeable {
    BacklogAndForecast merge(Mergeable backlog);

    BacklogAndForecast applyMerge(long otherBacklog, long otherForecast);

    BacklogAndForecast applyMerge(long otherBacklog, long otherForecast, Instant date);
  }

  @Value
  public static class QuantityWithEndDate implements BacklogAndForecast {

    long backlogQuantity;

    long forecastQuantity;

    Instant endDate;

    public QuantityWithEndDate(long backlogQuantity, long forecastQuantity, Instant endDate) {
      this.backlogQuantity = backlogQuantity;
      this.forecastQuantity = forecastQuantity;
      this.endDate = endDate;
    }

    private static Instant max(final Instant one, final Instant other) {
      return one.compareTo(other) > 0 ? one : other;
    }

    @Override
    public long total() {
      return backlogQuantity + forecastQuantity;
    }

    @Override
    public QuantityWithEndDate map(final LongUnaryOperator f) {
      return new QuantityWithEndDate(f.applyAsLong(backlogQuantity), f.applyAsLong(forecastQuantity), endDate);
    }

    @Override
    public BacklogAndForecast merge(final Mergeable backlog) {
      return backlog.applyMerge(this.backlogQuantity, this.forecastQuantity, endDate);
    }

    @Override
    public BacklogAndForecast applyMerge(final long otherBacklog, final long otherForecast) {
      return new QuantityWithEndDate(backlogQuantity + otherBacklog, forecastQuantity + otherForecast, endDate);
    }

    @Override
    public BacklogAndForecast applyMerge(final long otherBacklog, final long otherForecast, final Instant date) {
      final var maxEndDate = endDate == null || date == null ? null : max(endDate, date);
      return new QuantityWithEndDate(backlogQuantity + otherBacklog, forecastQuantity + otherForecast, maxEndDate);
    }

    @Override
    public long forecast() {
      return forecastQuantity;
    }

    @Override
    public long backlog() {
      return backlogQuantity;
    }
  }

  @Value
  public static class Quantity implements BacklogAndForecast {

    long backlogQuantity;

    long forecastQuantity;

    @Override
    public long total() {
      return backlogQuantity + forecastQuantity;
    }

    @Override
    public Quantity map(final LongUnaryOperator b) {
      return new Quantity(b.applyAsLong(backlogQuantity), b.applyAsLong(forecastQuantity));
    }

    @Override
    public BacklogAndForecast merge(final Mergeable backlog) {
      return backlog.applyMerge(this.backlogQuantity, this.forecastQuantity);
    }

    @Override
    public BacklogAndForecast applyMerge(final long otherBacklog, final long otherForecast) {
      return new Quantity(backlogQuantity + otherBacklog, forecastQuantity + otherForecast);
    }

    @Override
    public BacklogAndForecast applyMerge(final long otherBacklog, final long otherForecast, final Instant date) {
      return new QuantityWithEndDate(backlogQuantity + otherBacklog, forecastQuantity + otherForecast, date);
    }

    @Override
    public long forecast() {
      return forecastQuantity;
    }

    @Override
    public long backlog() {
      return backlogQuantity;
    }
  }


}
