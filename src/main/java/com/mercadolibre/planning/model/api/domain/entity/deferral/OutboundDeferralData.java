package com.mercadolibre.planning.model.api.domain.entity.deferral;

import com.mercadolibre.planning.model.api.domain.usecase.deferral.DeferralType;
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

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class OutboundDeferralData {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Column(name = "logistic_center_id", nullable = false)
  private String logisticCenterId;

  @Column(name = "date", nullable = false)
  private Instant date;

  @Column(name = "cpt", nullable = false)
  private Instant cpt;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private DeferralType status;

  @Column(name = "updated", nullable = false)
  private Boolean updated;

  public OutboundDeferralData(
      final String logisticCenterId,
      final Instant date,
      final Instant cpt,
      final DeferralType status,
      final boolean updated
  ) {
    this.logisticCenterId = logisticCenterId;
    this.date = date;
    this.cpt = cpt;
    this.status = status;
    this.updated = updated;
  }
}
