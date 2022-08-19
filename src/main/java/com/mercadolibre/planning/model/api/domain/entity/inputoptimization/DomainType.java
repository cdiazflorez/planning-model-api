package com.mercadolibre.planning.model.api.domain.entity.inputoptimization;

import static com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.inputdomain.DomainOptionFilter.INCLUDE_DAY_NAME;
import static com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.inputdomain.DomainOptionFilter.INCLUDE_PROCESS;
import static com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.inputdomain.DomainOptionFilter.INCLUDE_STAGE;
import static com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.inputdomain.DomainOptionFilter.INCLUDE_SHIFT_GROUP;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.inputdomain.DomainMultiple;
import com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.inputdomain.DomainOptionFilter;
import com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.inputdomain.DomainSingle;
import com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.inputdomain.DomainStrategy;
import com.mercadolibre.planning.model.api.exception.InvalidDomainFilterException;
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
public enum DomainType {
    ABSENCES(new DomainMultiple(), Absence.class),
    BACKLOG_BOUNDS(new DomainMultiple(), BacklogBound.class),
    CONFIGURATION(new DomainSingle(), Configuration.class),
    CONTRACT_MODALITY_TYPES(new DomainMultiple(), ContractModalityType.class),
    NON_SYSTEMIC_RATIO(new DomainMultiple(), NonSystemicRatio.class),
    POLYVALENCE_PARAMETERS(new DomainMultiple(), PolyvalenceParameter.class),
    PRESENCES(new DomainMultiple(), Presence.class),
    SHIFT_CONTRACT_MODALITIES(new DomainMultiple(), ShiftContractModality.class),
    SHIFTS_PARAMETERS(new DomainMultiple(), ShiftParameter.class),
    TRANSFERS(new DomainMultiple(), Transfer.class),
    WORKER_COSTS(new DomainMultiple(), WorkerCost.class),
    WORKERS_PARAMETERS(new DomainMultiple(), WorkersParameter.class);

    public final DomainStrategy domainStrategy;

    public final Class<? extends Domain> structure;

    private static final Map<String, DomainType> LOOKUP = Arrays.stream(values()).collect(
            toMap(DomainType::toString, Function.identity())
    );

    public static Optional<DomainType> of(final String value) {
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

        final Map<DomainOptionFilter, Function<List<Object>, Boolean>> shiftParametersFilters = Map.of(
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
                    final DomainOptionFilter domainOptionFilter = DomainOptionFilter.of(filterRequest.getKey())
                            .orElseThrow(() -> new InvalidDomainFilterException(SHIFTS_PARAMETERS, INCLUDE_DAY_NAME, INCLUDE_SHIFT_GROUP));
                    if (!shiftParametersFilters.containsKey(domainOptionFilter)) {
                        throw new InvalidDomainFilterException(SHIFTS_PARAMETERS, INCLUDE_DAY_NAME, INCLUDE_SHIFT_GROUP);
                    }
                    return shiftParametersFilters.get(domainOptionFilter).apply(filterRequest.getValue());
                })
                .reduce(true, Boolean::logicalAnd);
    }

    private static boolean nonSystemicRatioFilter(final NonSystemicRatio nonSystemicRatio,
                                                  final Map<String, List<Object>> domainFilterRequests) {

        final Map<DomainOptionFilter, Function<List<Object>, Boolean>> nonSystemicRatioFilters = Map.of(
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
                    final DomainOptionFilter domainOptionFilter = DomainOptionFilter.of(filterRequest.getKey())
                            .orElseThrow(() -> new InvalidDomainFilterException(NON_SYSTEMIC_RATIO, INCLUDE_PROCESS, INCLUDE_STAGE));
                    if (!nonSystemicRatioFilters.containsKey(domainOptionFilter)) {
                        throw new InvalidDomainFilterException(NON_SYSTEMIC_RATIO, INCLUDE_PROCESS, INCLUDE_STAGE);
                    }
                    return nonSystemicRatioFilters.get(domainOptionFilter).apply(filterRequest.getValue());
                })
                .reduce(true, Boolean::logicalAnd);
    }

    public interface Domain {
        default boolean conditionFilter(Map<String, List<Object>> domainFilterRequests) {
            return true;
        }
    }

    @Value
    private static class Absence implements Domain {

        String dayName;

        String shiftName;

        String contractModality;

        float unjustifiedAbsentRate;

        float justifiedAbsentRate;

    }

    @Value
    private static class BacklogBound implements Domain {

        String process;

        int stage;

        String stageName;

        int lowerBound;

        int upperBound;

    }

    @Value
    private static class Configuration implements Domain {

        boolean anticipateBacklog;

        boolean fixCptsOutbound;

        boolean fixSlasInbound;

        boolean generateValidationFiles;

        boolean activeTransfers;

        boolean activateHourlyWorkers;

        float hourlyWorkersCost;

    }

    @Value
    private static class ContractModalityType implements Domain {

        String contractModality;

        String contractModalityType;

    }

    @Value
    private static class NonSystemicRatio implements Domain {

        String process;

        String stage;

        List<ShiftRatio> shiftRatios;

        @Override
        public boolean conditionFilter(Map<String, List<Object>> domainFilterRequests) {
            return nonSystemicRatioFilter(this, domainFilterRequests);
        }
    }

    @Value
    private static class ShiftRatio {

        String key;

        String value;

    }

    @Value
    private static class PolyvalenceParameter implements Domain {

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
    private static class Presence implements Domain {

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
    private static class ShiftContractModality implements Domain {

        String shiftName;

        String contractModality;

    }

    @Value
    private static class ShiftParameter implements Domain {

        String dayName;

        String shiftName;

        String shiftGroup;

        String shiftType;

        int start;

        int end;

        @Override
        public boolean conditionFilter(Map<String, List<Object>> domainFilterRequests) {
            return shiftParametersFilter(this, domainFilterRequests);
        }

    }

    @Value
    private static class Transfer implements Domain {

        String shiftNameOrigin;

        String shiftNameDestination;

        String contractModality;

        float cost;

    }

    @Value
    private static class WorkerCost implements Domain {

        String shiftName;

        String contractModality;

        float hiringCost;

        float dismissalCost;

        float unitaryCost;

    }

    @Value
    private static class WorkersParameter implements Domain {

        String process;

        String contractModality;

        int stage;

        String shiftName;

        Integer workForce;

        Integer maxWorkers;

    }

}
