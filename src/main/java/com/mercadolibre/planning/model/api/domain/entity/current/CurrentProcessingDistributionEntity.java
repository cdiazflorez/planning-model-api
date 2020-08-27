package com.mercadolibre.planning.model.api.domain.entity.current;

import com.mercadolibre.planning.model.api.web.controller.request.MetricUnit;
import com.mercadolibre.planning.model.api.web.controller.request.ProcessName;
import com.mercadolibre.planning.model.api.web.controller.request.ProcessingType;
import com.mercadolibre.planning.model.api.web.controller.request.Workflow;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import java.time.ZonedDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "current_processing_distribution")
public class CurrentProcessingDistributionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private ZonedDateTime date;

    @Enumerated(EnumType.STRING)
    private Workflow workflow;

    @Enumerated(EnumType.STRING)
    private ProcessName processName;

    private long quantity;

    @Enumerated(EnumType.STRING)
    private MetricUnit quantityMetricUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "`type`")
    private ProcessingType type;

    private boolean isActive;
}
