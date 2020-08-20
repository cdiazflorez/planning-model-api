package com.mercadolibre.planning.model.api.dao.forecast;

import com.mercadolibre.planning.model.api.domain.MetricUnit;
import com.mercadolibre.planning.model.api.domain.ProcessName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
import javax.persistence.Table;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "headcount_distribution")
public class HeadcountDistributionDao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String area;

    @Enumerated(EnumType.STRING)
    private ProcessName processName;

    private long quantity;

    @Enumerated(EnumType.STRING)
    private MetricUnit quantityMetricUnit;

    @ManyToOne
    @JoinColumn(name = "forecast_id")
    @Fetch(FetchMode.SELECT)
    private ForecastDao forecast;
}
