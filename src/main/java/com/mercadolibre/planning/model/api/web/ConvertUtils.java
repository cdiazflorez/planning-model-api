package com.mercadolibre.planning.model.api.web;

import com.mercadolibre.planning.model.api.domain.usecase.entities.maxcapacity.get.MaxCapacityOutput;

import java.util.List;
import java.util.stream.Collectors;

public final class ConvertUtils {

    public static String toCsvFile(final List<MaxCapacityOutput> maxCapacities) {

        final String csvHead = "Logistic Center ID,Load date,Capacity date,Capacity quantity\n";
        final String csvBody = maxCapacities.stream().map(item -> String.join(",",
                        item.getLogisticCenterId(),
                        item.getLoadDate().toString(),
                        item.getMaxCapacityDate().toString(),
                        String.valueOf(item.getMaxCapacityValue())))
                .collect(Collectors.joining("\n"));

        return csvHead + csvBody;
    }
}
