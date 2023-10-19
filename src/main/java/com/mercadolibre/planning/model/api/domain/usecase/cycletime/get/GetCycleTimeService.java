package com.mercadolibre.planning.model.api.domain.usecase.cycletime.get;

import static java.lang.String.format;

import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.get.GetConfigurationsUseCase;
import com.mercadolibre.planning.model.api.exception.EntityNotFoundException;
import com.newrelic.api.agent.Trace;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class GetCycleTimeService {

  private static final String DEFAULT_CYCLE_TIME_KEY = "cycle_time";

  private static final String CYCLE_TIME_KEY_PATTERN = "cycle_time_%02d_%02d";

  private static final String CONFIGURATION_ENTITY = "configuration";

  private final GetConfigurationsUseCase getConfigurationsUseCase;

  @Trace
  public Map<ZonedDateTime, Long> execute(final GetCycleTimeInput input) {
    final List<Configuration> configurations = getConfigurationsUseCase.execute(input.getLogisticCenterId());

    final Map<ZonedDateTime, Long> ctByDateOut = new HashMap<>();

    input.getCptDates()
        .forEach(cptDate -> {
          final String cycleTimeKey = getCycleTimeKey(cptDate);

          final Long cycleTime = configurations.stream()
              .filter(configuration -> cycleTimeKey.equals(configuration.getKey()))
              .findFirst()
              .map(config -> Long.parseLong(config.getValue()))
              .orElseGet(() -> getCycleTimeDefault(configurations));

          ctByDateOut.put(cptDate, cycleTime);
        });

    return ctByDateOut;
  }

  private String getCycleTimeKey(final ZonedDateTime cptDate) {
    return format(CYCLE_TIME_KEY_PATTERN, cptDate.getHour(), cptDate.getMinute());
  }

  private Long getCycleTimeDefault(final List<Configuration> configurations) {
    return configurations.stream()
        .filter(configuration -> DEFAULT_CYCLE_TIME_KEY.equals(configuration.getKey()))
        .findFirst()
        .map(config -> Long.parseLong(config.getValue()))
        .orElseThrow(() -> new EntityNotFoundException(
            CONFIGURATION_ENTITY, DEFAULT_CYCLE_TIME_KEY));
  }
}
