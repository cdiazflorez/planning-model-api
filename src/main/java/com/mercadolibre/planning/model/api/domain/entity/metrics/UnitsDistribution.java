package com.mercadolibre.planning.model.api.domain.entity.metrics;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import java.time.ZonedDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Entity that contains the projected backlogs. */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class UnitsDistribution {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column
  private String logisticCenterId;

  @Column
  private ZonedDateTime date;

  @Enumerated(EnumType.STRING)
  @Column
  private ProcessName processName;

  @Column
  private String area;

  @Column
  private Double quantity;

  @Enumerated(EnumType.STRING)
  @Column
  private MetricUnit quantityMetricUnit;

  @Enumerated(EnumType.STRING)
  private Workflow workflow;
}
