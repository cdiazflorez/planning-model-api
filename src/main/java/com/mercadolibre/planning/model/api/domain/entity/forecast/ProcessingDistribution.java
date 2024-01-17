package com.mercadolibre.planning.model.api.domain.entity.forecast;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import java.time.ZonedDateTime;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
@Data
@Builder(builderClassName = "ProcessingDistBuilder")
@AllArgsConstructor
@NoArgsConstructor
public class ProcessingDistribution {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  private ZonedDateTime date;

  @Enumerated(EnumType.STRING)
  private ProcessPath processPath;

  @Enumerated(EnumType.STRING)
  private ProcessName processName;

  private double quantity;

  @Enumerated(EnumType.STRING)
  private MetricUnit quantityMetricUnit;

  @Enumerated(EnumType.STRING)
  private ProcessingType type;

  private String tags;

  @ManyToOne
  @JoinColumn(name = "forecast_id")
  @Fetch(FetchMode.SELECT)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Forecast forecast;
}
