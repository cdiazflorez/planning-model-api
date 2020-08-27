package com.mercadolibre.planning.model.api.domain.entity.forecast;

import com.mercadolibre.planning.model.api.web.controller.request.MetricUnit;
import com.mercadolibre.planning.model.api.web.controller.request.ProcessName;
import com.mercadolibre.planning.model.api.web.controller.request.ProcessingType;
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

import java.time.ZonedDateTime;

@Entity
@Getter
@Builder(builderClassName = "ProcessingDistBuilder")
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "processing_distribution")
public class ProcessingDistributionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private ZonedDateTime date;

    @Enumerated(EnumType.STRING)
    private ProcessName processName;

    private long quantity;

    @Enumerated(EnumType.STRING)
    private MetricUnit quantityMetricUnit;

    @Enumerated(EnumType.STRING)
    private ProcessingType type;

    @ManyToOne
    @JoinColumn(name = "forecast_id")
    @Fetch(FetchMode.SELECT)
    private ForecastEntity forecast;
}
