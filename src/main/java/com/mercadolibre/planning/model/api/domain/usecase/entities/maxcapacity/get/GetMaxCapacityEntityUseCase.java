package com.mercadolibre.planning.model.api.domain.usecase.entities.maxcapacity.get;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.forecast.MaxCapacityView;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static java.time.ZonedDateTime.parse;

@Service
@AllArgsConstructor
public class GetMaxCapacityEntityUseCase {

    private final ProcessingDistributionRepository processingDistRepository;

    public List<MaxCapacityOutput> execute(final Workflow workflow,
                                           final ZonedDateTime dateFrom,
                                           final ZonedDateTime dateTo) {

        final List<MaxCapacityView> maxCapacities = processingDistRepository
                .findMaxCapacitiesByDateInRange(null, workflow.name(), dateFrom, dateTo);

        return maxCapacities.stream()
                .map(item -> new MaxCapacityOutput(
                        item.getLogisticCenterId(),
                        parse(item.getLoadDate().toInstant().toString()),
                        parse(item.getMaxCapacityDate().toInstant().toString()),
                        item.getMaxCapacityValue()))
                .collect(Collectors.toList());
    }
}
