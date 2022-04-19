package com.mercadolibre.planning.model.api.domain.entity.forecast;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnitsDistribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column
    private String logisticCenterId;

    @Column
    private ZonedDateTime date;

    @Column
    private String processName;

    @Column
    private String area;

    @Column
    private Double quantity;

    @Enumerated(EnumType.STRING)
    @Column
    private MetricUnit quantityMetricUnit;
}
