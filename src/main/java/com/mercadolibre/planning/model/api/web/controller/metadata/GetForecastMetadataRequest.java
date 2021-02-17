package com.mercadolibre.planning.model.api.web.controller.metadata;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;

import java.time.ZonedDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class GetForecastMetadataRequest {

    @NotBlank
    private String warehouseId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private ZonedDateTime dateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private ZonedDateTime dateTo;

}
