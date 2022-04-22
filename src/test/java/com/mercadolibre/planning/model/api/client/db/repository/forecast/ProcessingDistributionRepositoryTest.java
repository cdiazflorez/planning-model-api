package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest(properties = {
    "spring.jpa.properties.hibernate.jdbc.time_zone=UTC"
})
@Sql({"/sql/load-processing-distributions.sql"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ProcessingDistributionRepositoryTest {

  @Autowired
  private ProcessingDistributionRepository repository;

  @Test
  public void findEntitiesByWarehouseIdWorkflowTypeProcessNameAndDateInRange_whenOnlyOneForecastIsPresent() {
    // WHEN
    final List<ProcessingDistributionView> result =
        repository.findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
            Set.of("PERFORMED_PROCESSING"),
            List.of("PICKING"),
            A_DATE_UTC,
            A_DATE_UTC.plusHours(3),
            List.of(1L)
        );

    // THEN
    assertNotNull(result);

    assertEquals(3, result.size());
    final var first = result.get(0);
    assertEquals(PICKING, first.getProcessName());
    assertEquals(A_DATE_UTC.toInstant(), first.getDate().toInstant());
    assertEquals(10, first.getQuantity());
    assertEquals(MetricUnit.UNITS, first.getQuantityMetricUnit());

    final var second = result.get(1);
    assertEquals(PICKING, second.getProcessName());
    assertEquals(A_DATE_UTC.plusHours(1).toInstant(), second.getDate().toInstant());
    assertEquals(20, second.getQuantity());
    assertEquals(MetricUnit.UNITS, second.getQuantityMetricUnit());

    final var third = result.get(2);
    assertEquals(PICKING, third.getProcessName());
    assertEquals(A_DATE_UTC.plusHours(2).toInstant(), third.getDate().toInstant());
    assertEquals(30, third.getQuantity());
    assertEquals(MetricUnit.UNITS, third.getQuantityMetricUnit());
  }

  @Test
  public void findEntitiesByWarehouseIdWorkflowTypeProcessNameAndDateInRange_whenMultipleForecastsArePresent() {
    // WHEN
    final List<ProcessingDistributionView> result =
        repository.findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
            Set.of("PERFORMED_PROCESSING"),
            List.of("PICKING"),
            A_DATE_UTC,
            A_DATE_UTC.plusHours(3),
            List.of(2L, 3L)
        );

    // THEN
    assertNotNull(result);

    assertEquals(3, result.size());
    final var first = result.get(0);
    assertEquals(PICKING, first.getProcessName());
    assertEquals(A_DATE_UTC.toInstant(), first.getDate().toInstant());
    assertEquals(1, first.getQuantity());
    assertEquals(MetricUnit.UNITS, first.getQuantityMetricUnit());

    final var second = result.get(1);
    assertEquals(PICKING, second.getProcessName());
    assertEquals(A_DATE_UTC.plusHours(1).toInstant(), second.getDate().toInstant());
    assertEquals(20, second.getQuantity());
    assertEquals(MetricUnit.UNITS, second.getQuantityMetricUnit());

    final var third = result.get(2);
    assertEquals(PICKING, third.getProcessName());
    assertEquals(A_DATE_UTC.plusHours(2).toInstant(), third.getDate().toInstant());
    assertEquals(30, third.getQuantity());
    assertEquals(MetricUnit.UNITS, third.getQuantityMetricUnit());
  }

}
