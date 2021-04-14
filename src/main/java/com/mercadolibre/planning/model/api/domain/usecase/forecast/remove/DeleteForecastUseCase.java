package com.mercadolibre.planning.model.api.domain.usecase.forecast.remove;

import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import com.mercadolibre.planning.model.api.exception.BadRequestException;
import com.mercadolibre.planning.model.api.gateway.ForecastGateway;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

import java.time.ZonedDateTime;

import static java.time.ZoneOffset.UTC;

@Service
@AllArgsConstructor
public class DeleteForecastUseCase implements UseCase<DeleteForecastInput, Integer> {

    private final ForecastGateway forecastGateway;

    @Transactional
    @Override
    public Integer execute(final DeleteForecastInput input) {
        if (input.getWeeks() < 0) {
            throw new BadRequestException("Week span should not be negative");
        }

        final ZonedDateTime limit = ZonedDateTime.now(UTC).minusWeeks(input.getWeeks());
        return forecastGateway.deleteOlderThan(input.getWorkflow(), limit);
    }
}
