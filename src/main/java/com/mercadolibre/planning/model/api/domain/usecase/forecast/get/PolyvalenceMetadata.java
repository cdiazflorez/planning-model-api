package com.mercadolibre.planning.model.api.domain.usecase.forecast.get;

import com.mercadolibre.planning.model.api.domain.entity.ProductivityPolyvalenceCardinality;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PolyvalenceMetadata {

  Map<ProductivityPolyvalenceCardinality, Float> polyvalences;

}
