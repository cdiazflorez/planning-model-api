package com.mercadolibre.planning.model.api.projection.waverless.idleness;

import com.mercadolibre.planning.model.api.projection.waverless.Wave;
import java.time.Instant;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import lombok.Value;

@AllArgsConstructor
@Value
public class DateWaveSupplier {
  Instant executionDate;
  Supplier<Wave> wave;
}
