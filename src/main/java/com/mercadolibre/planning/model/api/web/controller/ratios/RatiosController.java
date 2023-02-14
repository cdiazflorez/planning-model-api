package com.mercadolibre.planning.model.api.web.controller.ratios;

import com.mercadolibre.planning.model.api.domain.usecase.ratios.GetPackingWallRatiosUseCase;
import com.mercadolibre.planning.model.api.web.controller.ratios.response.GetPackingWallRatiosOutput;
import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/ratios")
@Slf4j
public class RatiosController {

  private final GetPackingWallRatiosUseCase getPackingWallRatiosUseCase;

  @GetMapping("/packing_wall")
  public ResponseEntity<Map<Instant, GetPackingWallRatiosOutput>> getPackingWallRatios(
      @RequestParam final String logisticCenterId,
      @RequestParam final Instant dateFrom,
      @RequestParam final Instant dateTo
  ) {
    if (dateTo.isBefore(dateFrom)) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
    }
    return ResponseEntity.ok(
        getPackingWallRatiosUseCase.execute(logisticCenterId, dateFrom, dateTo)
    );
  }
}
