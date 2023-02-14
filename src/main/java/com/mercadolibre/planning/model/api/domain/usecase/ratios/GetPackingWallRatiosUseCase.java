package com.mercadolibre.planning.model.api.domain.usecase.ratios;

import static java.util.Map.of;

import com.mercadolibre.planning.model.api.web.controller.ratios.response.GetPackingWallRatiosOutput;
import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class GetPackingWallRatiosUseCase {

  public Map<Instant, GetPackingWallRatiosOutput> execute(final String logisticCenterId, final Instant dateFrom, final Instant dateTo) {
    log.info("logisticCenterId: {}, dateFrom: {}, dateTo: {}", logisticCenterId, dateFrom, dateTo);
    return of(dateFrom, new GetPackingWallRatiosOutput(Double.NaN, Double.NaN));
  }
}
