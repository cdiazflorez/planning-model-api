package com.mercadolibre.planning.model.api.domain.entity.forecast;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
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
import java.util.List;

/**
 * The name is misleading. This is not a distribution but an element of a discrete distribution.
 * Instances of this class know the number of units that are predicted to be sold during the one-hour interval starting
 * at {@link #dateIn}, and the time at which all of them must have been shipped.
 */
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

    private double quantity;

    /**
     * TODO remove this field.
     */
    @Enumerated(EnumType.STRING)
    private MetricUnit quantityMetricUnit;

    @Enumerated(EnumType.STRING)
    private ProcessPath processPath;

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
    private List<PlanningDistributionMetadata> metadatas;
}
