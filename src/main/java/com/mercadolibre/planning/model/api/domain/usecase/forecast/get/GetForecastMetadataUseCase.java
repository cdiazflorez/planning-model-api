package com.mercadolibre.planning.model.api.domain.usecase.forecast.get;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ForecastMetadataRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ForecastMetadataView;
import com.mercadolibre.planning.model.api.domain.entity.WaveCardinality;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

import static com.mercadolibre.planning.model.api.util.DateUtils.getForecastWeeks;

@Service
@AllArgsConstructor
public class GetForecastMetadataUseCase implements UseCase<GetForecastMetadataInput,
        List<ForecastMetadataView>> {

    private final ForecastMetadataRepository forecastMetadataRepository;

    @Override
    public List<ForecastMetadataView> execute(final GetForecastMetadataInput input) {
        final Set<String> forecastWeeks = getForecastWeeks(input.getDateFrom(), input.getDateTo());
        return forecastMetadataRepository
                        .findLastForecastMetadataByWarehouseId(
                                List.of(
                                        WaveCardinality
                                                .MONO_ORDER_DISTRIBUTION.toJson(),
                                        WaveCardinality
                                                .MULTI_BATCH_DISTRIBUTION.toJson(),
                                        WaveCardinality
                                                .MULTI_ORDER_DISTRIBUTION.toJson()
                                ),
                                input.getWarehouseId(),
                                input.getWorkflow().name(),
                                forecastWeeks);
    }
}
