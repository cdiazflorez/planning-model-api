package com.mercadolibre.planning.model.api.web.controller.unitsdistibution.request;

import lombok.*;

import javax.validation.constraints.NotBlank;
import java.time.ZonedDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UnitsDistributionRequest {

    @NotBlank
    private String logisticCenterId;

    @NotBlank
    private ZonedDateTime date;

    @NotBlank
    private String processName;

    @NotBlank
    private String area;

    @NotBlank
    private Double quantity;

    @NotBlank
    private String quantityMetricUnit;
}
