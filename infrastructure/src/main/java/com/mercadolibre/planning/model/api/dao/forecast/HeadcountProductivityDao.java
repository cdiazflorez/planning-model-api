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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import java.time.OffsetTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "headcount_productivity")
public class HeadcountProductivityDao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private OffsetTime dayTime;

    private ProcessName processName;

    private long productivity;

    private MetricUnit productivityMetricUnit;

    private long abilityLevel;

    @ManyToOne
    @JoinColumn(name = "forecast_id")
    @Fetch(FetchMode.SELECT)
    private ForecastDao forecast;
}
