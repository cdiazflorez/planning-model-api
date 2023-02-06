package com.mercadolibre.planning.model.api.web.controller.ratios;

import com.mercadolibre.planning.model.api.domain.usecase.ratios.PreloadPackingWallRatiosUseCase;
import com.mercadolibre.planning.model.api.web.controller.RequestClock;
import com.mercadolibre.planning.model.api.web.controller.ratios.response.PreloadPackingRatiosOutput;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/ratios")
@Slf4j
public class RatiosJobController {

  private static final Duration DEFAULT_HOURS_LOOK_BACK = Duration.ofHours(1);

  private final PreloadPackingWallRatiosUseCase preloadRatiosUseCase;

  private final RequestClock requestClock;

  @PostMapping("/packing_wall/preload")
  public ResponseEntity<List<PreloadPackingRatiosOutput>> preloadPackingWallRatios(
      @RequestParam(required = false) Instant dateFrom,
      @RequestParam(required = false) Instant dateTo
  ) {
    final Instant requestDate = requestClock.now();
    final Instant calculatedDateFrom = dateFrom(dateFrom, requestDate);
    final Instant calculatedDateTo = dateTo(dateTo, requestDate);
    if (calculatedDateTo.isBefore(calculatedDateFrom)) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
    }

    return ResponseEntity.status(HttpStatus.OK)
        .body(preloadRatiosUseCase.execute(calculatedDateFrom, calculatedDateTo));
  }

  private Instant dateFrom(Instant dateFrom, Instant requestDate) {
    return dateFrom == null
        ? requestDate.minus(DEFAULT_HOURS_LOOK_BACK).truncatedTo(ChronoUnit.HOURS)
        : dateFrom.truncatedTo(ChronoUnit.HOURS);
  }

  private Instant dateTo(Instant dateTo, Instant requestDate) {
    return dateTo == null
        ? requestDate.truncatedTo(ChronoUnit.HOURS)
        : dateTo.truncatedTo(ChronoUnit.HOURS);
  }
}
