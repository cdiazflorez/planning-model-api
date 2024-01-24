package com.mercadolibre.planning.model.api.client.db.repository.current;

import static com.mercadolibre.planning.model.api.client.db.repository.util.StaffingPlanUtil.adaptResultToStaffingPlan;
import static com.mercadolibre.planning.model.api.client.db.repository.util.StaffingPlanUtil.buildColumnClause;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

import com.mercadolibre.planning.model.api.client.db.repository.util.StaffingPlanUtil;
import com.mercadolibre.planning.model.api.domain.entity.plan.CurrentStaffingPlanInput;
import com.mercadolibre.planning.model.api.domain.entity.plan.StaffingPlan;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CurrentProcessingDistributionJpaRepository {

  private static final String DEFAULT_COLUMNS = "sum(quantity) as quantity, quantity_metric_unit, type %s";

  private static final String DEFAULT_WHERE = """
      date between :dateFrom and :dateTo
      and type = :type
      and logistic_center_id = :logisticCenterId
      and workflow = :workflow
      and is_active = true
      """;

  private static final String DEFAULT_GROUP_BY = "quantity_metric_unit, type %s";

  private final EntityManager entityManager;

  public List<StaffingPlan> getCurrentStaffingPlan(final CurrentStaffingPlanInput input) {

    final Query query = entityManager.createNativeQuery(buildQuery(input));

    final List result = addParameters(query, input).getResultList();

    return adaptResultToStaffingPlan(result, input.groupers());
  }

  private String buildQuery(final CurrentStaffingPlanInput input) {
    final String columns = buildColumnClause(DEFAULT_COLUMNS, input.groupers());
    final String groupBy = buildColumnClause(DEFAULT_GROUP_BY, input.groupers());

    String where = DEFAULT_WHERE + input.filters().keySet()
        .stream()
        .map(StaffingPlanUtil::buildWhereClauseWithJsonExtract)
        .collect(joining());

    return format("SELECT %s FROM current_processing_distribution WHERE %s GROUP BY %s", columns, where, groupBy);
  }

  private static Query addParameters(final Query query, final CurrentStaffingPlanInput input) {
    query.setParameter("dateFrom", input.dateFrom());
    query.setParameter("dateTo", input.dateTo());
    query.setParameter("type", input.type().toJson());
    query.setParameter("logisticCenterId", input.logisticCenterId());
    query.setParameter("workflow", input.workflow().getName());

    input.filters().forEach(query::setParameter);

    return query;
  }

}
