package com.mercadolibre.planning.model.api.domain.usecase.deferral;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeferralDto {

    private String warehouseId;

    private String workflow;

    private List<ProjectionDto> projections;
}
