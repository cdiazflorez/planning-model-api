package com.mercadolibre.planning.model.api.web.controller.configuration.request;

import javax.validation.constraints.NotEmpty;

public record ConfigurationRequestDto(
    @NotEmpty
    String key,
    @NotEmpty
    String value
) {
}
