package com.mercadolibre.planning.model.api.domain.entity;

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
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProcessNameToProcessPath {

    PACKING(List.of(TOT_MONO, TOT_MULTI_ORDER)),
    BATCH_SORTER(List.of(TOT_MULTI_BATCH));

    private static final Map<String, ProcessNameToProcessPath> LOOKUP = Arrays.stream(values()).collect(
            toMap(ProcessNameToProcessPath::toString, Function.identity())
    );
    private final List<ProcessPath> paths;

    public static Optional<ProcessNameToProcessPath> of(final String value) {
        return Optional.ofNullable(LOOKUP.get(value.toUpperCase(Locale.US).replace('-', '_')));
    }

    @JsonValue
    public String toJson() {
        return toString().toLowerCase(Locale.US);
    }

    public String getName() {
        return name().toLowerCase(Locale.ROOT);
    }

}
