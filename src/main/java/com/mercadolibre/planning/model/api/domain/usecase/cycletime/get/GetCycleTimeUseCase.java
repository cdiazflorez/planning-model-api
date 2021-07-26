package com.mercadolibre.planning.model.api.domain.usecase.cycletime.get;

import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import com.mercadolibre.planning.model.api.exception.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

@Service
@AllArgsConstructor
public class GetCycleTimeUseCase implements UseCase<GetCycleTimeInput, Configuration> {

    private static final String DEFAULT_PROCESSING_TIME_KEY = "processing_time";
    private static final String CYCLE_TIME_KEY_PATTERN = "cycle_time_%02d_%02d";
    private static final String CONFIGURATION_ENTITY = "configuration";

    @Override
    public Configuration execute(final GetCycleTimeInput input) {
        final List<Configuration> configurations = input.getConfigurations();
        final String cycleTimeKey = getCycleTimeKey(input.getCptDate());

        final Optional<Configuration> cycleTimeConfig = configurations.stream()
                .filter(configuration -> cycleTimeKey.equals(configuration.getKey()))
                .findFirst();

        return cycleTimeConfig.orElseGet(() -> getDefaultProcessingTime(configurations));
    }

    private String getCycleTimeKey(final ZonedDateTime cptDate) {
        return format(CYCLE_TIME_KEY_PATTERN, cptDate.getHour(), cptDate.getMinute());
    }

    private Configuration getDefaultProcessingTime(final List<Configuration> configurations) {
        return configurations.stream()
                .filter(configuration -> DEFAULT_PROCESSING_TIME_KEY.equals(configuration.getKey()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        CONFIGURATION_ENTITY, DEFAULT_PROCESSING_TIME_KEY));
    }
}
