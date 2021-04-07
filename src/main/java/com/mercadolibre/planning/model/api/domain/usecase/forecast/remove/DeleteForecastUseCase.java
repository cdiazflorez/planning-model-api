package com.mercadolibre.planning.model.api.domain.usecase.forecast.remove;

import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import com.mercadolibre.planning.model.api.exception.BadRequestException;
import com.mercadolibre.planning.model.api.gateway.ForecastGateway;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

import java.time.ZonedDateTime;

@Service
@AllArgsConstructor
public class DeleteForecastUseCase implements UseCase<DeleteForecastInput, Integer> {

    private final ForecastGateway forecastGateway;

    @Transactional
    @Override
    public Integer execute(final DeleteForecastInput input) {
        if (input.getDays() < 0) {
            throw new BadRequestException("Day span should not be negative");
        }

        final ZonedDateTime lastMonth = ZonedDateTime.now().minusDays(input.getDays());
        return forecastGateway.deleteOlderThan(input.getWorkflow(), lastMonth);
    }
}
