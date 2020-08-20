package com.mercadolibre.planning.model.api.dao.current;

import com.mercadolibre.planning.model.api.domain.MetricUnit;
import com.mercadolibre.planning.model.api.domain.ProcessName;
import com.mercadolibre.planning.model.api.domain.Workflow;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
@Table(name = "current_headcount_productivity")
public class CurrentHeadcountProductivityDao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private ZonedDateTime date;

    @Enumerated(EnumType.STRING)
    private Workflow workflow;

    private ProcessName processName;

    private long productivity;

    private MetricUnit productivityMetricUnit;

    private long abilityLevel;

    private boolean isActive;
}
