package com.mercadolibre.planning.model.api.domain.usecase.cptbywarehouse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteCoverageResult {
    private Canalization canalization;
    private String status;
}
