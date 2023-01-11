package com.mercadolibre.planning.model.api.domain.entity;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MULTI_ORDER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_ORDER;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProcessNameToProcessPath {

    WAVING(List.of(TOT_MONO, NON_TOT_MONO, TOT_MULTI_BATCH, NON_TOT_MULTI_BATCH, TOT_MULTI_ORDER, NON_TOT_MULTI_ORDER)),
    PICKING(List.of(TOT_MONO, NON_TOT_MONO, TOT_MULTI_BATCH, NON_TOT_MULTI_BATCH, TOT_MULTI_ORDER, NON_TOT_MULTI_ORDER)),
    PACKING(List.of(TOT_MONO, NON_TOT_MONO, TOT_MULTI_ORDER, NON_TOT_MULTI_ORDER)),
    WALL_IN(List.of(TOT_MULTI_BATCH, NON_TOT_MULTI_BATCH)),
    PACKING_WALL(List.of(TOT_MULTI_BATCH, NON_TOT_MULTI_BATCH)),
    BATCH_SORTER(List.of(TOT_MULTI_BATCH, NON_TOT_MULTI_BATCH));

    private static final Map<String, ProcessNameToProcessPath> LOOKUP = Arrays.stream(values()).collect(
            toMap(ProcessNameToProcessPath::toString, Function.identity())
    );
    private final List<ProcessPath> paths;

    public static Optional<ProcessNameToProcessPath> of(final String value) {
        return Optional.ofNullable(LOOKUP.get(value.toUpperCase(Locale.US).replace('-', '_')));
    }

    public static List<ProcessNameToProcessPath> getTriggersProcess() {
        return Arrays.stream(ProcessNameToProcessPath.values())
                .filter(processNameToProcessPath -> !processNameToProcessPath.getName().equals(ProcessName.WAVING.getName()))
                .filter(processNameToProcessPath -> !processNameToProcessPath.getName().equals(ProcessName.PICKING.getName()))
                .collect(Collectors.toUnmodifiableList());
    }

    @JsonValue
    public String toJson() {
        return toString().toLowerCase(Locale.US);
    }

    public String getName() {
        return name().toLowerCase(Locale.ROOT);
    }

}
