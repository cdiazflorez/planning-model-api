package com.mercadolibre.planning.model.api.web.controller.deferral;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class DeferralResponse {

  private final int status;

  private final String message;
}
