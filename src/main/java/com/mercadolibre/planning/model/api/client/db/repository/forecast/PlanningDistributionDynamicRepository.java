package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.Grouper;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanningDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanningDistributionRepository;
import com.newrelic.api.agent.Trace;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PlanningDistributionDynamicRepository implements PlanningDistributionRepository {
  private final EntityManager entityManager;

  @Trace
  @Override
  public List<PlanningDistribution> findByWarehouseIdWorkflowAndDateOutAndDateInInRange(
      final Instant dateInFrom,
      final Instant dateInTo,
      final Instant dateOutFrom,
      final Instant dateOutTo,
      final Set<ProcessPath> processPaths,
      final Set<Grouper> groupers,
      final Set<Long> forecastIds) {

    final Query query = buildQuery(groupers, dateInFrom, dateOutFrom);

    final var resultQuery = parametersQuery(query, forecastIds, processPaths, dateInFrom, dateInTo, dateOutFrom, dateOutTo)
        .getResultList();

    return adaptResultQueryToPlanningDistribution(resultQuery, groupers);
  }

  private Query buildQuery(final Set<Grouper> groupers,
                           final Instant dateInFrom,
                           final Instant dateOutFrom) {
    final SqlQuery sqlQuery = new SqlQuery();

    groupers.forEach(grouper -> {
      sqlQuery.withSelect(GrouperRepository.valueOf(grouper.name()).getSelect());
      sqlQuery.withGroupBy(GrouperRepository.valueOf(grouper.name()).getGroupBy());
    });

    sqlQuery.withSelect("SUM(quantity) as quantity");

    if (dateInFrom != null) {
      sqlQuery.withWhere("date_in BETWEEN :date_in_from AND :date_in_to");
    } else if (dateOutFrom != null) {
      sqlQuery.withWhere("date_out BETWEEN :date_out_from AND :date_out_to");
    }

    sqlQuery.withWhere("process_path IN (:process_paths)");

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
    query.setParameter("process_paths", processPaths.stream().map(Enum::name).collect(Collectors.toList()));

    if (dateInFrom != null) {
      query.setParameter("date_in_from", dateInFrom);
      query.setParameter("date_in_to", dateInTo);
    } else if (dateOutFrom != null) {
      query.setParameter("date_out_from", dateOutFrom);
      query.setParameter("date_out_to", dateOutTo);
    }

    return query;
  }

  private List<PlanningDistribution> adaptResultQueryToPlanningDistribution(final List resultQuery, final Set<Grouper> groupers) {

    return ((List<Object[]>) resultQuery).stream()
        .map(row -> mappingRowQueryToPlanningDistribution(row, groupers))
        .collect(Collectors.toList());
  }

  private PlanningDistribution mappingRowQueryToPlanningDistribution(final Object[] row, final Set<Grouper> groupers) {
    final PlanningDistribution planningDistribution = new PlanningDistribution();
    final AtomicInteger index = new AtomicInteger(0);

    planningDistribution.setForecastId(((BigInteger) row[index.get()]).longValue());
    index.incrementAndGet();

    for (Grouper grouper : groupers) {
      switch (grouper) {
        case DATE_IN:
          planningDistribution.setDateIn(((Timestamp) row[index.get()]).toInstant());
          break;
        case DATE_OUT:
          planningDistribution.setDateOut(((Timestamp) row[index.get()]).toInstant());
          break;
        case PROCESS_PATH:
          planningDistribution.setProcessPath(ProcessPath.valueOf((String) row[index.get()]));
          break;
        default:
          break;
      }

      index.incrementAndGet();
    }

    planningDistribution.setQuantity(((BigDecimal) row[index.get()]).doubleValue());

    return planningDistribution;
  }

  @AllArgsConstructor
  @Getter
  private enum GrouperRepository {
    DATE_OUT("date_out as dateOut", "date_out"),
    DATE_IN("date_in  as dateIn", "date_in"),
    PROCESS_PATH("process_path as processPath", "process_path");

    private String select;
    private String groupBy;

  }

  private static class SqlQuery {
    private static final String DELIMITER = ", ";
    private static final String AND = " AND ";

    private final List<String> selectExpressions;
    private final List<String> whereExpressions;
    private final List<String> groupByExpressions;

    SqlQuery() {
      selectExpressions = new ArrayList<>();
      whereExpressions = new ArrayList<>();
      groupByExpressions = new ArrayList<>();
    }

    void withSelect(final String s) {
      selectExpressions.add(s);
    }

    void withWhere(final String s) {
      whereExpressions.add(s);
    }

    void withGroupBy(final String s) {
      groupByExpressions.add(s);
    }

    String completeQuery() {
      return "SELECT forecast_id as forecastId, " + String.join(DELIMITER, selectExpressions)
          + " FROM planning_distribution p"
          + " WHERE p.forecast_id in (:forecast_ids) AND " + String.join(AND, whereExpressions)
          + " group by forecast_id, " + String.join(DELIMITER, groupByExpressions);
    }

  }
}
