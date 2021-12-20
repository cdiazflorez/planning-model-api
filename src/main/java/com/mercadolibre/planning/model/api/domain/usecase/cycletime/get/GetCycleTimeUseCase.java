package com.mercadolibre.planning.model.api.domain.usecase.cycletime.get;

import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.get.GetConfigurationsUseCase;
import com.mercadolibre.planning.model.api.exception.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;

@Service
@AllArgsConstructor
public class GetCycleTimeUseCase implements
        UseCase<GetCycleTimeInput, Map<ZonedDateTime, Configuration>> {

    private static final String DEFAULT_CYCLE_TIME_KEY = "cycle_time";
    private static final String CYCLE_TIME_KEY_PATTERN = "cycle_time_%02d_%02d";
    private static final String CONFIGURATION_ENTITY = "configuration";

    private final GetConfigurationsUseCase getConfigurationsUseCase;

    @Override
    public Map<ZonedDateTime, Configuration> execute(final GetCycleTimeInput input) {

        final List<Configuration> configurations = getConfigurationsUseCase
                .execute(input.getLogisticCenterId());

        final Map<ZonedDateTime, Configuration> ctByDateOut = new HashMap<>();

        input.getCptDates().forEach(cptDate -> {

            final String cycleTimeKey = getCycleTimeKey(cptDate);

            final Optional<Configuration> cycleTimeConfig = configurations.stream()
                    .filter(configuration -> cycleTimeKey.equals(configuration.getKey()))
                    .findFirst();

            ctByDateOut.put(cptDate,
                    cycleTimeConfig.orElseGet(() -> getCycleTimeDefault(configurations)));
        });

        return ctByDateOut;
    }

    private String getCycleTimeKey(final ZonedDateTime cptDate) {
        return format(CYCLE_TIME_KEY_PATTERN, cptDate.getHour(), cptDate.getMinute());
    }

    private Configuration getCycleTimeDefault(final List<Configuration> configurations) {
        return configurations.stream()
                .filter(configuration -> DEFAULT_CYCLE_TIME_KEY.equals(configuration.getKey()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        CONFIGURATION_ENTITY, DEFAULT_CYCLE_TIME_KEY));
    }
}
