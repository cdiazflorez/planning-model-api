package com.mercadolibre.planning.model.api.domain.usecase.sla;

import static java.time.format.DateTimeFormatter.ofPattern;

import com.mercadolibre.metrics.MetricCollector.Tags;
import com.mercadolibre.metrics.Metrics;
import java.time.ZonedDateTime;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum ExecutionMetrics {
  USES_OF_PROCESSING_TIME_DEFAULT("processing-time.default");

  private static final String FLOW_PREFIX = "application.planning.model.api.%s";

  private static final String CPT_TAG = "cptHour";

  private static final String LOGISTIC_CENTER_ID = "logisticCenterID";

  private final String metric;

  ExecutionMetrics(final String sufix) {
    metric = String.format(FLOW_PREFIX, sufix);
  }

  public void count(final Tags... tagList) {
    final Tags tags = new Tags();
    Arrays.stream(tagList).forEach(tags::addAll);
    DataDogMetricsWrapper.count(metric, tags);
  }

  public static Tags withLogisticCenterID(final String logisticCenter) {
    return new Tags().add(LOGISTIC_CENTER_ID, logisticCenter);
  }

  public static Tags withCPTHour(final ZonedDateTime cpt) {
    return new Tags().add(CPT_TAG, cpt.format(ofPattern("HH")));
  }

  public static final class DataDogMetricsWrapper {
    private static final Metrics PROXY = Metrics.INSTANCE;

    private DataDogMetricsWrapper() {
    }

    static void count(String var1, Tags tags) {
      PROXY.incrementCounter(var1, tags);
    }
  }
}
