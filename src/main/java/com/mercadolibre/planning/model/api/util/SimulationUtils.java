package com.mercadolibre.planning.model.api.util;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.web.controller.request.EntityType;
import com.mercadolibre.planning.model.api.web.controller.simulation.Simulation;
import com.mercadolibre.planning.model.api.web.controller.simulation.SimulationEntity;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;

public class SimulationUtils {

    public static Map<ProcessName, Map<ZonedDateTime, Long>> createSimulationMap(
            final List<Simulation> simulations,
            final EntityType entityType) {

        if (simulations == null) {
            return emptyMap();
        }

        return simulations.stream().collect(toMap(
                Simulation::getProcessName,
                simulation -> simulation.getEntities().stream()
                        .filter(entity -> entity.getType() == entityType)
                        .map(SimulationEntity::getValues)
                        .flatMap(Collection::stream)
                        .collect(toMap(
                                o -> o.getDate().withFixedOffsetZone(),
                                o -> Long.valueOf(o.getQuantity()),
                                (int1, int2) -> int2)
                        ),
                (map1, map2) -> map2));
    }
}
