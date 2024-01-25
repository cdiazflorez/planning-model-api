package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import static com.mercadolibre.planning.model.api.client.db.repository.util.StaffingPlanUtil.adaptResultToStaffingPlan;
import static com.mercadolibre.planning.model.api.client.db.repository.util.StaffingPlanUtil.buildColumnClause;
import static com.mercadolibre.planning.model.api.client.db.repository.util.StaffingPlanUtil.buildWhereClauseWithJsonExtract;
import static com.mercadolibre.planning.model.api.util.EntitiesUtil.paginate;
import static java.lang.String.format;

import com.mercadolibre.planning.model.api.domain.entity.forecast.ProcessingDistribution;
import com.mercadolibre.planning.model.api.domain.entity.plan.StaffingPlan;
import com.mercadolibre.planning.model.api.domain.entity.plan.StaffingPlanInput;
import com.mercadolibre.planning.model.api.gateway.ProcessingDistributionGateway;
import com.newrelic.api.agent.Trace;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ProcessingDistributionJpaRepository implements ProcessingDistributionGateway {
  private static final String COLUMN_NAMES = "forecast_id, date, process_path, process_name, quantity, quantity_metric_unit, type, tags";

  private static final String DEFAULT_COLUMNS = "sum(quantity) as quantity, quantity_metric_unit, type %s, forecast_id";

  private static final String DEFAULT_WHERE = "forecast_id in (:forecastIds) and date between :dateFrom and :dateTo and type = :type";

  private static final String DEFAULT_GROUP_BY = "forecast_id, quantity_metric_unit, type %s";

  private static final String DEFAULT_PARTITION_BY =
      "ROW_NUMBER() OVER (PARTITION BY quantity_metric_unit, type %s ORDER BY forecast_id DESC) AS r";

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

  private static Query addParameters(final Query query, final StaffingPlanInput staffingPlanInput) {
    query.setParameter("forecastIds", staffingPlanInput.forecastIds());
    query.setParameter("dateFrom", staffingPlanInput.dateFrom());
    query.setParameter("dateTo", staffingPlanInput.dateTo());
    query.setParameter("type", staffingPlanInput.type().toJson());

    staffingPlanInput.filters().forEach(query::setParameter);

    return query;
  }

}
