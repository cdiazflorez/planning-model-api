package com.mercadolibre.planning.model.api.domain.entity.inputoptimization;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.IdClass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity(name = "inputs_optimization")
@AllArgsConstructor
@NoArgsConstructor
@IdClass(InputOptimizationId.class)
public class InputOptimization {

    @Id
    private String warehouseId;

    @Id
    @Enumerated(EnumType.STRING)
    private DomainType domain;

    private String jsonValue;

}
