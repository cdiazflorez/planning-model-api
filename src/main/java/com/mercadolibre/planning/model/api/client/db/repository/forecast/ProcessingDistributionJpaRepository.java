package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import static com.mercadolibre.planning.model.api.util.EntitiesUtil.paginate;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ProcessingDistribution;
import com.mercadolibre.planning.model.api.domain.entity.plan.StaffingPlan;
import com.mercadolibre.planning.model.api.domain.entity.plan.StaffingPlanInput;
import com.mercadolibre.planning.model.api.gateway.ProcessingDistributionGateway;
import com.newrelic.api.agent.Trace;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ProcessingDistributionJpaRepository implements ProcessingDistributionGateway {
  private static final String COLUMN_NAMES = "forecast_id, date, process_path, process_name, quantity, quantity_metric_unit, type, tags";

  private static final String DEFAULT_COLUMNS = "forecast_id, sum(quantity) as quantity, quantity_metric_unit, type %s";

  private static final String DEFAULT_WHERE = "forecast_id in (:forecastIds) and date between :dateFrom and :dateTo and type = :type";

  private static final String DEFAULT_GROUP_BY = "forecast_id, quantity_metric_unit, type %s";

  private static final String DEFAULT_PARTITION_BY =
      "ROW_NUMBER() OVER (PARTITION BY quantity_metric_unit, type %s ORDER BY forecast_id DESC) AS r";

  private static final String DATE_COLUMN = "date";

  private static final String TAGS_COLUMN = "tags ->>'$.%s'";

  private static final String COMMA_SEPARATOR = ", ";

  private static final String EMPTY_STRING = "";

  private static final int COLUMN_FORECAST_ID = 0;

  private static final int COLUMN_QUANTITY = 1;

  private static final int COLUMN_QUANTITY_METRIC_UNIT = 2;

  private static final int COLUMN_TYPE = 3;

  private static final int NEXT_COLUMN = 4;

  private static final String COLUMN_PLACEHOLDERS = "(?,?,?,?,?,?,?,?),";

  private static final int INSERT_SIZE = 1000;

  private final EntityManager entityManager;

  @Trace
  @Override
  public void create(final List<ProcessingDistribution> entities, final long forecastId) {
    final List<List<ProcessingDistribution>> pages = paginate(entities, INSERT_SIZE);

    pages.forEach(page -> {
      final Query query = entityManager.createNativeQuery(getInsertQuery(page.size()));

      int paramIndex = 1;

      for (final ProcessingDistribution entity : page) {
        query.setParameter(paramIndex++, forecastId);
        query.setParameter(paramIndex++, entity.getDate());
        query.setParameter(paramIndex++, entity.getProcessPath().name());
        query.setParameter(paramIndex++, entity.getProcessName().name());
        query.setParameter(paramIndex++, entity.getQuantity());
        query.setParameter(paramIndex++, entity.getQuantityMetricUnit().name());
        query.setParameter(paramIndex++, entity.getType().name());
        query.setParameter(paramIndex++, entity.getTags());
      }
      query.executeUpdate();
    });
  }

  private String getInsertQuery(final int size) {
    final String query = "INSERT INTO processing_distribution (" + COLUMN_NAMES + ") VALUES " + COLUMN_PLACEHOLDERS.repeat(size);
    return query.substring(0, query.length() - 1);
  }

  public List<StaffingPlan> getStaffingPlan(final StaffingPlanInput staffingPlanInput) {

    final Query query = entityManager.createNativeQuery(buildQuery(staffingPlanInput));

    final var result = addParameters(query, staffingPlanInput).getResultList();

    return adaptResultToStaffingPlan(result, staffingPlanInput.groupers());
  }

  private static String buildWhereClauseWithJsonExtract(final String column) {
    return format(" AND tags ->> '$.%s' in (:%s)", column, column);
  }

  private static String buildQuery(final StaffingPlanInput staffingPlanInput) {

    final String columns = buildColumnClause(DEFAULT_COLUMNS, staffingPlanInput.groupers());

    final String groupBy = buildColumnClause(DEFAULT_GROUP_BY, staffingPlanInput.groupers());

    final String partitionBy = buildColumnClause(DEFAULT_PARTITION_BY, staffingPlanInput.groupers());

    String where = DEFAULT_WHERE;

    for (String keyFilter : staffingPlanInput.filters().keySet()) {
      where = where.concat(buildWhereClauseWithJsonExtract(keyFilter));
    }

    return format(
        "SELECT * FROM (SELECT %s, %s FROM processing_distribution WHERE %s GROUP BY %s) as pd WHERE r = 1",
        columns,
        partitionBy,
        where,
        groupBy
    );
  }

  private static String buildColumnClause(final String clause,
                                          final List<String> groupers) {
    return !groupers.isEmpty()
        ? format(clause, COMMA_SEPARATOR.concat(groupers.stream()
                                                    .map(grouper -> DATE_COLUMN.equals(grouper)
                                                        ? DATE_COLUMN : format(TAGS_COLUMN, grouper))
                                                    .collect(joining(COMMA_SEPARATOR))))
        : format(clause, EMPTY_STRING);
  }

  private static Query addParameters(final Query query, final StaffingPlanInput staffingPlanInput) {
    query.setParameter("forecastIds", staffingPlanInput.forecastIds());
    query.setParameter("dateFrom", staffingPlanInput.dateFrom());
    query.setParameter("dateTo", staffingPlanInput.dateTo());
    query.setParameter("type", staffingPlanInput.type().toJson());

    staffingPlanInput.filters().forEach(query::setParameter);

    return query;
  }

  private List<StaffingPlan> adaptResultToStaffingPlan(final List resultQuery,
                                                       final List<String> groupers) {

    return ((List<Object[]>) resultQuery).stream()
        .map(row -> {

          final double quantity = (Double) row[COLUMN_QUANTITY];
          final MetricUnit metricUnit = MetricUnit.valueOf(((String) row[COLUMN_QUANTITY_METRIC_UNIT]));
          final ProcessingType type = ProcessingType.valueOf((String) row[COLUMN_TYPE]);

          final ConcurrentHashMap<String, String> groupersMap = new ConcurrentHashMap<>();

          groupers.forEach(grouper -> {
            final Object rowValue = row[NEXT_COLUMN + groupers.indexOf(grouper)];
            if (rowValue != null) {
              if (DATE_COLUMN.equals(grouper)) {
                groupersMap.put(grouper, ((Timestamp) rowValue).toInstant().toString());
              } else {
                groupersMap.put(grouper, (String) rowValue);
              }
            }
          });
          return new StaffingPlan(quantity, metricUnit, type, groupersMap);
        })
        .toList();
  }
}
