package com.mercadolibre.planning.model.api.domain.usecase.cptbywarehouse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class Canalization {
    private String id;
    private List<CarrierServiceId> carrierServices;
}
