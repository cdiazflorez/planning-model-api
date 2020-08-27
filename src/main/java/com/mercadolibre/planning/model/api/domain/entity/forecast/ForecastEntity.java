package com.mercadolibre.planning.model.api.domain.entity.forecast;

import com.mercadolibre.planning.model.api.web.controller.request.Workflow;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Table(name = "forecast")
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class ForecastEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Enumerated(EnumType.STRING)
    @NotNull
    private Workflow workflow;

    @Column(updatable = false)
    @CreationTimestamp
    private ZonedDateTime dateCreated;

    @UpdateTimestamp
    private ZonedDateTime lastUpdated;

    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 50)
    @OneToMany(mappedBy = "forecast", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<HeadcountDistributionEntity> headcountDistributions = new HashSet<>();

    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 100)
    @OneToMany(mappedBy = "forecast", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<HeadcountProductivityEntity> headcountProductivities = new HashSet<>();

    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 100)
    @OneToMany(mappedBy = "forecast", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<PlanningDistributionEntity> planningDistributions = new HashSet<>();

    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 200)
    @OneToMany(mappedBy = "forecast", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<ProcessingDistributionEntity> processingDistributions = new HashSet<>();

    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 100)
    @OneToMany(
            mappedBy = "forecastId",
            cascade = CascadeType.ALL,
            fetch = FetchType.EAGER,
            orphanRemoval = true)
    @Builder.Default
    private Set<ForecastMetadataEntity> metadatas = new HashSet<>();
}
