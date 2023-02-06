package com.mercadolibre.planning.model.api.domain.usecase.ratios;

import static java.util.Collections.emptyList;

import com.mercadolibre.planning.model.api.web.controller.ratios.response.PreloadPackingRatiosOutput;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class PreloadPackingWallRatiosUseCase {

  public List<PreloadPackingRatiosOutput> execute(Instant dateFrom, Instant dateTo) {
    log.info("dateFrom: {}, dateTo: {}", dateFrom, dateTo);
    return emptyList();
  }
}
