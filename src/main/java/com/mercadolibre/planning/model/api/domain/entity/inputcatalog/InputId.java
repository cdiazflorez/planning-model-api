package com.mercadolibre.planning.model.api.domain.entity.inputcatalog;

import static com.mercadolibre.planning.model.api.domain.usecase.inputcatalog.inputdomain.InputOptionFilter.INCLUDE_DAY_NAME;
import static com.mercadolibre.planning.model.api.domain.usecase.inputcatalog.inputdomain.InputOptionFilter.INCLUDE_PROCESS;
import static com.mercadolibre.planning.model.api.domain.usecase.inputcatalog.inputdomain.InputOptionFilter.INCLUDE_SHIFT_GROUP;
import static com.mercadolibre.planning.model.api.domain.usecase.inputcatalog.inputdomain.InputOptionFilter.INCLUDE_STAGE;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mercadolibre.planning.model.api.domain.usecase.inputcatalog.inputdomain.InputMultiple;
import com.mercadolibre.planning.model.api.domain.usecase.inputcatalog.inputdomain.InputOptionFilter;
import com.mercadolibre.planning.model.api.domain.usecase.inputcatalog.inputdomain.InputSingle;
import com.mercadolibre.planning.model.api.domain.usecase.inputcatalog.inputdomain.InputStrategy;
import com.mercadolibre.planning.model.api.exception.InvalidInputFilterException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@RequiredArgsConstructor
public enum InputId {
    ABSENCES(new InputMultiple(), Absence.class),
    BACKLOG_BOUNDS(new InputMultiple(), BacklogBound.class),
    CONFIGURATION(new InputSingle(), Configuration.class),
    CONTRACT_MODALITY_TYPES(new InputMultiple(), ContractModalityType.class),
    NON_SYSTEMIC_RATIO(new InputMultiple(), NonSystemicRatio.class),
    POLYVALENCE_PARAMETERS(new InputMultiple(), PolyvalenceParameter.class),
    PRESENCES(new InputMultiple(), Presence.class),
    SHIFT_CONTRACT_MODALITIES(new InputMultiple(), ShiftContractModality.class),
    SHIFTS_PARAMETERS(new InputMultiple(), ShiftParameter.class),
    TRANSFERS(new InputMultiple(), Transfer.class),
    WORKER_COSTS(new InputMultiple(), WorkerCost.class),
    WORKERS_PARAMETERS(new InputMultiple(), WorkersParameter.class);

    public final InputStrategy inputStrategy;

    public final Class<? extends InputCatalog> structure;

    private static final Map<String, InputId> LOOKUP = Arrays.stream(values()).collect(
            toMap(InputId::toString, Function.identity())
    );

    public static Optional<InputId> of(final String value) {
        return ofNullable(LOOKUP.get(value.toUpperCase(Locale.getDefault())));
    }

    @JsonValue
    public String toJson() {
        return toString().toLowerCase(Locale.getDefault());
    }

    private static List<String> convertObjectListToStringList(final List<Object> objectList) {
        return objectList.stream().map(Object::toString).collect(Collectors.toList());
    }

    private static boolean shiftParametersFilter(final ShiftParameter shiftParameter,
                                                 final Map<String, List<Object>> domainFilterRequests) {

        final Map<InputOptionFilter, Function<List<Object>, Boolean>> shiftParametersFilters = Map.of(
                INCLUDE_DAY_NAME, (List<Object> objectList) -> {
                    final List<String> dayNames = convertObjectListToStringList(objectList);
                    return dayNames.contains(shiftParameter.getDayName());
                },
                INCLUDE_SHIFT_GROUP, (List<Object> objectList) -> {
                    final List<String> shiftGroups = convertObjectListToStringList(objectList);
                    return shiftGroups.contains(shiftParameter.getShiftGroup());
                }
        );

        return domainFilterRequests.entrySet().stream()
                .map(filterRequest -> {
                    final InputOptionFilter inputOptionFilter = InputOptionFilter.of(filterRequest.getKey())
                            .orElseThrow(() -> new InvalidInputFilterException(SHIFTS_PARAMETERS, INCLUDE_DAY_NAME, INCLUDE_SHIFT_GROUP));
                    if (!shiftParametersFilters.containsKey(inputOptionFilter)) {
                        throw new InvalidInputFilterException(SHIFTS_PARAMETERS, INCLUDE_DAY_NAME, INCLUDE_SHIFT_GROUP);
                    }
                    return shiftParametersFilters.get(inputOptionFilter).apply(filterRequest.getValue());
                })
                .reduce(true, Boolean::logicalAnd);
    }

    private static boolean nonSystemicRatioFilter(final NonSystemicRatio nonSystemicRatio,
                                                  final Map<String, List<Object>> domainFilterRequests) {

        final Map<InputOptionFilter, Function<List<Object>, Boolean>> nonSystemicRatioFilters = Map.of(
                INCLUDE_PROCESS, (List<Object> objectList) -> {
                    final List<String> process = convertObjectListToStringList(objectList);
                    return process.contains(nonSystemicRatio.getProcess());
                },
                INCLUDE_STAGE, (List<Object> objectList) -> {
                    final List<String> subProcess = convertObjectListToStringList(objectList);
                    return subProcess.contains(nonSystemicRatio.getStage());
                }
        );

        return domainFilterRequests.entrySet().stream()
                .map(filterRequest -> {
                    final InputOptionFilter inputOptionFilter = InputOptionFilter.of(filterRequest.getKey())
                            .orElseThrow(() -> new InvalidInputFilterException(NON_SYSTEMIC_RATIO, INCLUDE_PROCESS, INCLUDE_STAGE));
                    if (!nonSystemicRatioFilters.containsKey(inputOptionFilter)) {
                        throw new InvalidInputFilterException(NON_SYSTEMIC_RATIO, INCLUDE_PROCESS, INCLUDE_STAGE);
                    }
                    return nonSystemicRatioFilters.get(inputOptionFilter).apply(filterRequest.getValue());
                })
                .reduce(true, Boolean::logicalAnd);
    }

    public interface InputCatalog {
        default boolean conditionFilter(Map<String, List<Object>> inputFilterRequests) {
            return true;
        }
    }

    @Value
    private static class Absence implements InputCatalog {

        String dayName;

        String shiftName;

        String contractModality;

        float unjustifiedAbsentRate;

        float justifiedAbsentRate;

    }

    @Value
    private static class BacklogBound implements InputCatalog {

        String process;

        int stage;

        String stageName;

        int lowerBound;

        int upperBound;

    }

    @Value
    private static class Configuration implements InputCatalog {

        boolean anticipateBacklog;

        boolean fixCptsOutbound;

        boolean fixSlasInbound;

        boolean generateValidationFiles;

        boolean activeTransfers;

        boolean activateHourlyWorkers;

        float hourlyWorkersCost;

    }

    @Value
    private static class ContractModalityType implements InputCatalog {

        String contractModality;

        String contractModalityType;

    }

    @Value
    private static class NonSystemicRatio implements InputCatalog {

        String process;

        String stage;

        List<ShiftRatio> shiftRatios;

        @Override
        public boolean conditionFilter(Map<String, List<Object>> inputFilterRequests) {
            return nonSystemicRatioFilter(this, inputFilterRequests);
        }
    }

    @Value
    private static class ShiftRatio {

        String key;

        String value;

    }

    @Value
    private static class PolyvalenceParameter implements InputCatalog {

        String processOrigin;

        int stageOrigin;

        String processDestination;

        int stageDestination;

        String shiftName;

        String contractModality;

        int workForce;

        int maxWorkers;

    }

    @Value
    private static class Presence implements InputCatalog {

        String process;

        String shiftName;

        String contractModality;

        int hour;

        @JsonProperty("Mon")
        float mon;

        @JsonProperty("Tue")
        float tue;

        @JsonProperty("Wed")
        float wed;

        @JsonProperty("Thu")
        float thu;

        @JsonProperty("Fri")
        float fri;

        @JsonProperty("Sat")
        float sat;

        @JsonProperty("Sun")
        float sun;

    }

    @Value
    private static class ShiftContractModality implements InputCatalog {

        String shiftName;

        String contractModality;

    }

    @Value
    private static class ShiftParameter implements InputCatalog {

        String dayName;

        String shiftName;

        String shiftGroup;

        String shiftType;

        int start;

        int end;

        @Override
        public boolean conditionFilter(Map<String, List<Object>> inputFilterRequests) {
            return shiftParametersFilter(this, inputFilterRequests);
        }

    }

    @Value
    private static class Transfer implements InputCatalog {

        String shiftNameOrigin;

        String shiftNameDestination;

        String contractModality;

        float cost;

    }

    @Value
    private static class WorkerCost implements InputCatalog {

        String shiftName;

        String contractModality;

        float hiringCost;

        float dismissalCost;

        float unitaryCost;

    }

    @Value
    private static class WorkersParameter implements InputCatalog {

        String process;

        String contractModality;

        int stage;

        String shiftName;

        Integer workForce;

        Integer maxWorkers;

    }

}
