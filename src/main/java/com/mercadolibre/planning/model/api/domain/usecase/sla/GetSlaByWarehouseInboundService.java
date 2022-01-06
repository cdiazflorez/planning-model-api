package com.mercadolibre.planning.model.api.domain.usecase.sla;

import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseInput;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseOutput;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GetSlaByWarehouseInboundService implements GetSlaByWarehouseService {

    @Override
    public List<GetSlaByWarehouseOutput> execute(final GetSlaByWarehouseInput input) {
        return input.getDafaultSlas() == null
                ? Collections.emptyList()
                : input.getDafaultSlas()
                .stream()
                .filter(date -> !input.getCptTo().isBefore(date))
                .distinct()
                .sorted()
                .map(sla ->
                        GetSlaByWarehouseOutput.builder()
                                .logisticCenterId(input.getLogisticCenterId())
                                .date(sla)
                                .build()
                ).collect(Collectors.toList());

    }


}
