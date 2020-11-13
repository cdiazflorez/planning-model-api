package com.mercadolibre.planning.model.api.util;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.output.EntityOutput;
import com.mercadolibre.planning.model.api.web.controller.request.Source;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EntitiesUtil {

    public static Map<ProcessName, Map<ZonedDateTime, Map<Source, EntityOutput>>>
            toMapByProcessNameDateAndSource(final List<EntityOutput> entities) {

        return entities.stream().collect(Collectors.groupingBy(
                EntityOutput::getProcessName,
                TreeMap::new,
                Collectors.groupingBy(
                        o -> o.getDate().withFixedOffsetZone(),
                        TreeMap::new,
                        Collectors.toMap(
                                EntityOutput::getSource,
                                Function.identity(),
                                (e1, e2) -> e2
                        )
                )
        ));
    }

    public static Map<ProcessName, Map<ZonedDateTime, EntityOutput>> toMapByProcessNameAndDate(
            final List<EntityOutput> entities) {

        return entities.stream().collect(Collectors.groupingBy(
                EntityOutput::getProcessName,
                TreeMap::new,
                Collectors.toMap(
                        o -> o.getDate().withFixedOffsetZone(),
                        Function.identity(),
                        (e1, e2) -> e2
                )
        ));
    }
}
