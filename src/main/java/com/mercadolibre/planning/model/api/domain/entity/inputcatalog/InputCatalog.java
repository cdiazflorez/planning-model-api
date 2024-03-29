package com.mercadolibre.planning.model.api.domain.entity.inputcatalog;

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
@IdClass(InputCatalogId.class)
public class InputCatalog {

    @Id
    private String warehouseId;

    @Id
    @Enumerated(EnumType.STRING)
    private InputId domain;

    private String jsonValue;

}
