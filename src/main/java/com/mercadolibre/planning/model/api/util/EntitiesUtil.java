package com.mercadolibre.planning.model.api.util;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.web.controller.projection.request.Source;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Math.ceil;
import static java.lang.Math.min;

public class EntitiesUtil {

    public static <T extends EntityOutput> Map<ProcessName, Map<ZonedDateTime, Map<Source, T>>>
            toMapByProcessNameDateAndSource(final List<T> entities) {

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

    public static <T extends EntityOutput> Map<ProcessName, Map<ZonedDateTime, T>>
        toMapByProcessNameAndDate(final Stream<T> entities) {

        return entities.collect(Collectors.groupingBy(
                EntityOutput::getProcessName,
                TreeMap::new,
                Collectors.toMap(
                        o -> o.getDate().withFixedOffsetZone(),
                        Function.identity(),
                        (e1, e2) -> e2
                )
        ));
    }

    public static List<List<?>> paginate(final List<?> entities, final int pageSize) {
        final int pagesQuantity = (int) ceil((double) entities.size() / (double)pageSize);
        final List<List<?>> pages = new ArrayList<>();

        IntStream.rangeClosed(1, pagesQuantity).forEach(i -> {
            final int offset = (i - 1) * pageSize;
            final int limit = min(offset + pageSize, entities.size());
            pages.add(entities.subList(offset, limit));
        });
        return pages;
    }
}
