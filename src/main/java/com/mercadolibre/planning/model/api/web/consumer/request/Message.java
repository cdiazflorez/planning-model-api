package com.mercadolibre.planning.model.api.web.consumer.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @NotNull
    private String warehouseId;
}
