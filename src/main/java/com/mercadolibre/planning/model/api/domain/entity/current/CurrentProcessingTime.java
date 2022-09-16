package com.mercadolibre.planning.model.api.domain.entity.current;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
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
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class CurrentProcessingTime {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Enumerated(EnumType.STRING)
  private Workflow workflow;

  private String logisticCenterId;

  @Column(name = "`value`")
  private int value;

  @Enumerated(EnumType.STRING)
  private MetricUnit metricUnit;

  private ZonedDateTime cptFrom;

  private ZonedDateTime cptTo;

  @CreationTimestamp
  private ZonedDateTime dateCreated;

  @UpdateTimestamp
  private ZonedDateTime lastUpdated;

  private Long userId;
}
