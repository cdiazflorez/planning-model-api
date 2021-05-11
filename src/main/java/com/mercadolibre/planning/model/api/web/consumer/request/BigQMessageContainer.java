package com.mercadolibre.planning.model.api.web.consumer.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BigQMessageContainer {

    @Valid
    @NotNull
    private Message msg;
}
