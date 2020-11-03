package com.mercadolibre.planning.model.api.domain.entity.forecast;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import java.time.ZonedDateTime;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class HeadcountProductivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private ZonedDateTime date;

    @Enumerated(EnumType.STRING)
    private ProcessName processName;

    private long productivity;

    @Enumerated(EnumType.STRING)
    private MetricUnit productivityMetricUnit;

    private int abilityLevel;

    @ManyToOne
    @JoinColumn(name = "forecast_id")
    @Fetch(FetchMode.SELECT)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Forecast forecast;
}
