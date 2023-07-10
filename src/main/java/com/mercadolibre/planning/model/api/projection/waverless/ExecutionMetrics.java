package com.mercadolibre.planning.model.api.projection.waverless;

import com.mercadolibre.metrics.Metrics;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum ExecutionMetrics {
  UPSTREAM_ITERATIONS("upstream.iterations"),
  SLA_DATE_ITERATIONS("sla.iterations");

  private static final String FLOW_PREFIX = "application.planning.model.api.waverless.%s";

  private final String metric;

  ExecutionMetrics(final String sufix) {
    metric = String.format(FLOW_PREFIX, sufix);
  }

  public void count(final long val) {
    DataDogMetricsWrapper.histogram(metric, val);
  }

  public static final class DataDogMetricsWrapper {
    private static final Metrics PROXY = Metrics.INSTANCE;

    private DataDogMetricsWrapper() {
    }

    static void histogram(String var1, long var2, String... var4) {
      PROXY.histogram(var1, var2, var4);
    }
  }
}
