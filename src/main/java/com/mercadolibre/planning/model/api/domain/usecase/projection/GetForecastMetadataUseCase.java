package com.mercadolibre.planning.model.api.domain.usecase.projection;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ForecastMetadataRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ForecastMetadataView;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetForecastMetadataInput;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class GetForecastMetadataUseCase implements UseCase<GetForecastMetadataInput,
        List<ForecastMetadataView>> {

    private final ForecastMetadataRepository forecastMetadataRepository;

    @Override
    public List<ForecastMetadataView> execute(final GetForecastMetadataInput input) {
        return forecastMetadataRepository
                        .findLastForecastMetadataByWarehouseId(input.getWarehouseId());
    }
}
