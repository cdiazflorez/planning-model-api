package com.mercadolibre.planning.model.api.domain.entity.forecast;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import java.time.ZonedDateTime;
import java.util.Set;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PlanningDistribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private ZonedDateTime dateIn;

    private ZonedDateTime dateOut;

    private long quantity;

    @Enumerated(EnumType.STRING)
    private MetricUnit quantityMetricUnit;

    @ManyToOne
    @JoinColumn(name = "forecast_id")
    private Forecast forecast;

    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 100)
    @OneToMany(
            mappedBy = "planningDistributionId",
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<PlanningDistributionMetadata> metadatas;
}
