package com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DayDto {

    private String etDay;

    private String etHour;

    private String processingTime;

    private String type;

    private boolean shouldDeferral;

    public boolean isShouldDeferral() {
        return this.shouldDeferral;
    }

}
