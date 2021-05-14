package com.mercadolibre.planning.model.api.domain.usecase.deferral;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectionDto {

    private ZonedDateTime estimatedTimeDeparture;

    private boolean shouldDeferral;
}
