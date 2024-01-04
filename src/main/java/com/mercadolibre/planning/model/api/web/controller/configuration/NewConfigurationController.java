package com.mercadolibre.planning.model.api.web.controller.configuration;

import static com.mercadolibre.planning.model.api.util.DateUtils.validateDateRangeIsMaxDays;
import static com.mercadolibre.planning.model.api.util.DateUtils.validateDatesRanges;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.service.configuration.DayAndHourProcessingTime;
import com.mercadolibre.planning.model.api.domain.service.configuration.ProcessingTimeService;
import com.mercadolibre.planning.model.api.domain.service.configuration.SlaProcessingTimes;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.ConfigurationUseCase;
import com.mercadolibre.planning.model.api.exception.DateRangeException;
import com.newrelic.api.agent.Trace;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/logistic_center/{logisticCenterId}/configuration")
public class NewConfigurationController {

  private static final int DAYS_IN_WEEK = 7;

  private final ConfigurationUseCase configurationUseCase;

  private final ProcessingTimeService processingTimeService;

  @Trace(dispatcher = true)
  @PostMapping
  public ResponseEntity<Map<String, String>> save(
      @PathVariable final String logisticCenterId,
      @RequestParam final long userId,
      @RequestBody @Valid final Map<@NotBlank String, @NotBlank String> configurations) {

    return ResponseEntity.ok(
        toResponse(configurationUseCase.save(userId, logisticCenterId, configurations))
    );
  }

  @Trace(dispatcher = true)
  @GetMapping
  public ResponseEntity<Map<String, String>> get(@PathVariable final String logisticCenterId,
                                                 @RequestParam(required = false, defaultValue = "") final Set<String> keys) {

    final var configurations = configurationUseCase.get(logisticCenterId, keys);

    if (configurations.isEmpty()) {
      return ResponseEntity.noContent().build();
    }

    return ResponseEntity.ok(toResponse(configurations));

  }

  @GetMapping("/processing_time")
  @Trace(dispatcher = true)
  public ResponseEntity<SlaProcessingTimes> getProcessingTimeForCptInDateRange(
      @PathVariable final String logisticCenterId,
      @RequestParam final Instant dateFrom,
      @RequestParam final Instant dateTo,
      @RequestParam final ZoneId zoneId
  ) {

    validateProcessingTimeDatesInWeek(dateFrom, dateTo);

    return ResponseEntity.ok(processingTimeService.getProcessingTimeForCptInDateRange(logisticCenterId, dateFrom, dateTo, zoneId));
  }

  @PostMapping("/processing_time")
  @Trace(dispatcher = true)
  public ResponseEntity<List<DayAndHourProcessingTime>> updateProcessingTimeForCptsByLogisticCenter(
      @PathVariable final String logisticCenterId
  ) {
    return ResponseEntity.ok(processingTimeService.updateProcessingTimeForCptsByLogisticCenter(logisticCenterId));
  }

  private Map<String, String> toResponse(final List<Configuration> configurations) {
    return configurations.stream()
        .collect(
            toMap(
                Configuration::getKey,
                Configuration::getValue,
                (o1, o2) -> o2
            )
        );
  }


  private void validateProcessingTimeDatesInWeek(final Instant dateFrom, final Instant dateTo) {
    if (!validateDatesRanges(dateFrom, dateTo)) {
      throw new DateRangeException(dateFrom, dateTo);
    }
    validateDateRangeIsMaxDays(dateFrom, dateTo, DAYS_IN_WEEK);
  }


}
