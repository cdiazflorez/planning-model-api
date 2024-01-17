package com.mercadolibre.planning.model.api.util;

import static com.mercadolibre.planning.model.api.domain.usecase.sla.ExecutionMetrics.USES_OF_PROCESSING_TIME_DEFAULT;
import static com.mercadolibre.planning.model.api.domain.usecase.sla.ExecutionMetrics.withCPTHour;
import static com.mercadolibre.planning.model.api.domain.usecase.sla.ExecutionMetrics.withLogisticCenterID;

import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseOutput;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public final class GetSlaByWarehouseUtils {

    private GetSlaByWarehouseUtils() {
    }

    public static List<GetSlaByWarehouseOutput> addBacklogInSlaInOrder(
            final List<GetSlaByWarehouseOutput> backlog,
            final List<GetSlaByWarehouseOutput> slas) {


        final HashSet<ZonedDateTime> slaDates = slas.stream()
                .map(GetSlaByWarehouseOutput::getDate)
                .collect(Collectors.toCollection(HashSet::new));

        final List<GetSlaByWarehouseOutput> slaFilter = new ArrayList<>(slas);
        final var slasToAdd = backlog.stream()
                .filter(b -> !slaDates.contains(b.getDate()))
            .toList();

        for (GetSlaByWarehouseOutput sla : slasToAdd) {
            USES_OF_PROCESSING_TIME_DEFAULT.count(
                withLogisticCenterID(sla.getLogisticCenterId()),
                withCPTHour(sla.getDate())
            );
        }

        slaFilter.addAll(slasToAdd);

        return slaFilter.stream()
                .sorted(Comparator.comparing(GetSlaByWarehouseOutput::getDate))
            .toList();

    }
}
