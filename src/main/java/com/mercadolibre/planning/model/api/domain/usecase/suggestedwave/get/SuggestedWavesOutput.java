package com.mercadolibre.planning.model.api.domain.usecase.suggestedwave.get;

import com.mercadolibre.planning.model.api.domain.entity.WaveCardinality;
import lombok.Value;

@Value
public class SuggestedWavesOutput {

    private WaveCardinality waveCardinality;
    private long quantity;
}
