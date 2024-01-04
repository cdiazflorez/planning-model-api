package com.mercadolibre.planning.model.api.domain.entity.configuration;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class OutboundProcessingTime {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Column(name = "logistic_center_id", nullable = false)
  private String logisticCenterID;

  @Column(name = "etd_day", nullable = false)
  private String etdDay;

  @Column(name = "etd_hour", nullable = false)
  private String etdHour;

  @Column(name = "etd_processing_time", nullable = false)
  private int etdProcessingTime;

  @Column(
      name = "date_created",
      nullable = false,
      updatable = false,
      columnDefinition = "TIMESTAMP default CURRENT_TIMESTAMP"
  )
  @CreationTimestamp
  private Instant dateCreated;

  @Column(
      name = "date_updated", nullable = false, columnDefinition = "TIMESTAMP default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP")
  @UpdateTimestamp
  private Instant dateUpdated;

  @Column(name = "is_active", nullable = false)
  private boolean isActive;

  @Transient
  private Integer parsedEtdHour;

  public OutboundProcessingTime(
      final String logisticCenterID,
      final String etdDay,
      final String etdHour,
      final int etdProcessingTime,
      final boolean isActive
  ) {
    this.logisticCenterID = logisticCenterID;
    this.etdDay = etdDay;
    this.etdHour = etdHour;
    this.etdProcessingTime = etdProcessingTime;
    this.isActive = isActive;
  }

  public Integer getParsedEtdHour() {
    if (parsedEtdHour == null && etdHour != null) {
      parsedEtdHour = Integer.parseInt(etdHour);
    }

    return parsedEtdHour;
  }
}
