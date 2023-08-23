package com.mercadolibre.planning.model.api.web.controller.deviation.response;

import java.util.List;
import lombok.Value;

@Value
public class DeviationsResponse {

  List<GetForecastDeviationResponse> deviations;

}
