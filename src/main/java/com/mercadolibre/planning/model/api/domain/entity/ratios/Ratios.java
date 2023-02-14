package com.mercadolibre.planning.model.api.domain.entity.ratios;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import java.time.Instant;
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
import org.hibernate.annotations.CreationTimestamp;

/** Entity that contains the packing backlogs ratios. */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Ratios {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column
  private Workflow workflow;

  @Column
  private String logisticCenterId;

  @Column
  private String type;

  @Column
  private Instant date;

  @Column(name = "`value`")
  private double value;

  @CreationTimestamp
  private Instant createdAt;

  public Ratios(Workflow workflow, String logisticCenterId, String type, Instant date, double value) {
    this.workflow = workflow;
    this.logisticCenterId = logisticCenterId;
    this.type = type;
    this.date = date;
    this.value = value;
  }
}
