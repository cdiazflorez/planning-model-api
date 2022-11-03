package com.mercadolibre.planning.model.api.domain.entity.plan;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Entity that contains the request and response of SPA from CC. */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalDialogSpa {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column
  private Instant requestDate;

  @Column
  private String spaRequest;

  @Column
  private String spaResponse;

  @Column
  private String frontResult;

  @Column
  private Instant responseDate;

  @Column
  private String logisticCenter;
}
