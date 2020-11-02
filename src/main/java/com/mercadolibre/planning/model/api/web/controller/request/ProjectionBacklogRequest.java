package com.mercadolibre.planning.model.api.web.controller.request;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
public class ProjectionBacklogRequest {

    private ZonedDateTime date;

    private int quantity;
}
