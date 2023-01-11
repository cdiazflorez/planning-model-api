package com.mercadolibre.planning.model.api.domain.entity.forecast;

import com.mercadolibre.planning.model.api.domain.entity.DeviationType;
import com.mercadolibre.planning.model.api.domain.entity.Path;
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
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CurrentForecastDeviation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  private String logisticCenterId;

  private ZonedDateTime dateFrom;

  private ZonedDateTime dateTo;

  @Column(name = "`value`")
  private double value;

  private boolean isActive;

  private Long userId;

  @Enumerated(EnumType.STRING)
  private Workflow workflow;

  @CreationTimestamp
  private ZonedDateTime dateCreated;

  @UpdateTimestamp
  private ZonedDateTime lastUpdated;

  @Enumerated(EnumType.STRING)
  private DeviationType type;

  @Enumerated(EnumType.STRING)
  private Path path;
}
