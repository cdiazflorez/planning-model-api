package com.mercadolibre.planning.model.api.web.controller.configuration.request;

import javax.validation.constraints.NotBlank;

public record ConfigurationRequestDto(
    @NotBlank
    String key,
    @NotBlank
    String value
) {
}
