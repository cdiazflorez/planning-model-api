package com.mercadolibre.planning.model.api.domain.usecase.entities.maxcapacity.get;

import static java.time.ZonedDateTime.parse;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.forecast.MaxCapacityView;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class GetMaxCapacityByWarehouseEntityUseCase {

    private final ProcessingDistributionRepository processingDistRepository;

    public List<MaxCapacityOutput> execute(final String warehouse,
                                           final ZonedDateTime dateFrom,
                                           final ZonedDateTime dateTo) {
        final List<MaxCapacityView> maxCapacities = processingDistRepository
                .findMaxCapacitiesByWarehouseAndDateInRange(warehouse, dateFrom, dateTo);

        return maxCapacities.stream()
                .map(item -> new MaxCapacityOutput(
                        item.getLogisticCenterId(),
                        parse(item.getLoadDate().toInstant().toString()),
                        parse(item.getMaxCapacityDate().toInstant().toString()),
                        item.getMaxCapacityValue()))
                .collect(Collectors.toList());
    }
}
