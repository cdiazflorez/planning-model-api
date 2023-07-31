package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanningDistributionGateway;
import com.newrelic.api.agent.Trace;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PlanningDistributionDynamicRepository implements PlanningDistributionGateway {

  private static final int COLUMN_FORECAST_ID = 0;

  private static final int COLUMN_DATE_IN = 1;

  private static final int COLUMN_DATE_OUT = 2;

  private static final int COLUMN_QUANTITY = 3;

  private static final int COLUMN_METRIC_UNIT = 4;

  private static final int COLUMN_PROCESS_PATH = 5;

  private final EntityManager entityManager;

  @Trace
  @Override
  public List<PlanDistribution> findByForecastIdsAndDynamicFilters(
      final Instant dateInFrom,
      final Instant dateInTo,
      final Instant dateOutFrom,
      final Instant dateOutTo,
      final Set<ProcessPath> processPaths,
      final Set<Long> forecastIds) {

    final Query query = buildQuery(dateInFrom, dateInTo, dateOutFrom, processPaths);

    final var resultQuery = parametersQuery(query, forecastIds, processPaths, dateInFrom, dateInTo, dateOutFrom, dateOutTo)
        .getResultList();

    return adaptResultQueryToPlanningDistribution(resultQuery);
  }

  private Query buildQuery(final Instant dateInFrom,
                           final Instant dateInTo,
                           final Instant dateOutFrom,
                           final Set<ProcessPath> processPaths) {
    final SqlQuery sqlQuery = new SqlQuery();

    if (dateInFrom != null) {
      sqlQuery.withWhere(":date_in_from <= p.date_in");
    }

    if (dateInTo != null) {
      sqlQuery.withWhere("p.date_in <= :date_in_to");
    }

    if (dateOutFrom != null) {
      sqlQuery.withWhere("date_out BETWEEN :date_out_from AND :date_out_to");
    }

    if (!processPaths.isEmpty()) {
      sqlQuery.withWhere("p.process_path IN (:process_paths)");
    }

    return entityManager.createNativeQuery(sqlQuery.completeQuery());
  }

  private Query parametersQuery(
      final Query query,
      final Set<Long> forecastIds,
      final Set<ProcessPath> processPaths,
      final Instant dateInFrom,
      final Instant dateInTo,
      final Instant dateOutFrom,
      final Instant dateOutTo) {

    query.setParameter("forecast_ids", forecastIds);

    if (!processPaths.isEmpty()) {
      query.setParameter("process_paths", processPaths.stream().map(Enum::name).collect(Collectors.toList()));
    }

    if (dateInFrom != null) {
      query.setParameter("date_in_from", dateInFrom);
    }

    if (dateInTo != null) {
      query.setParameter("date_in_to", dateInTo);
    }

    if (dateOutFrom != null) {
      query.setParameter("date_out_from", dateOutFrom);
      query.setParameter("date_out_to", dateOutTo);
    }

    return query;
  }

  private List<PlanDistribution> adaptResultQueryToPlanningDistribution(final List resultQuery) {

    return ((List<Object[]>) resultQuery).stream()
        .map(row -> new PlanDistribution(
            ((BigInteger) row[COLUMN_FORECAST_ID]).longValue(),
            ((Timestamp) row[COLUMN_DATE_IN]).toInstant(),
            ((Timestamp) row[COLUMN_DATE_OUT]).toInstant(),
            ProcessPath.valueOf((String) row[COLUMN_PROCESS_PATH]),
            MetricUnit.valueOf((String) row[COLUMN_METRIC_UNIT]),
            ((Number) row[COLUMN_QUANTITY]).doubleValue()
        ))
        .collect(Collectors.toList());
  }

  private static class SqlQuery {
    private static final String AND = " AND ";
    private final List<String> whereExpressions;

    SqlQuery() {
      whereExpressions = new ArrayList<>();
    }

    void withWhere(final String s) {
      whereExpressions.add(s);
    }

    String completeQuery() {
      return "SELECT p.forecast_id, p.date_in, p.date_out, p.quantity, p.quantity_metric_unit, p.process_path"
          + " FROM planning_distribution p"
          + " WHERE p.forecast_id in (:forecast_ids) AND " + String.join(AND, whereExpressions)
          + " ORDER BY p.forecast_id desc";
    }

  }
}
