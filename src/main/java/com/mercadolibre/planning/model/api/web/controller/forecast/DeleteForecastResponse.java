package com.mercadolibre.planning.model.api.web.controller.forecast;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class DeleteForecastResponse {
    private final Integer deletedRows;
}
