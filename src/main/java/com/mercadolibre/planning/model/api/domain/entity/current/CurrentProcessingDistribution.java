package com.mercadolibre.planning.model.api.domain.entity.current;


import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CurrentProcessingDistribution {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  private ZonedDateTime date;

  @Enumerated(EnumType.STRING)
  private Workflow workflow;

  @Enumerated(EnumType.STRING)
  private ProcessPath processPath;

  @Enumerated(EnumType.STRING)
  private ProcessName processName;

  private long quantity;

  @Enumerated(EnumType.STRING)
  private MetricUnit quantityMetricUnit;

  @Enumerated(EnumType.STRING)
  @Column(name = "`type`")
  private ProcessingType type;

  private boolean isActive;

  private String logisticCenterId;

  private long userId;

  @CreationTimestamp
  private ZonedDateTime dateCreated;

  @UpdateTimestamp
  private ZonedDateTime lastUpdated;

  public ProcessPath getProcessPath() {
    return processPath == null ? ProcessPath.GLOBAL : processPath;
  }
}
