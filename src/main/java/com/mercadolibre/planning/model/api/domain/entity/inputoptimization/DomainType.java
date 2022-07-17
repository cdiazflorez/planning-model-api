package com.mercadolibre.planning.model.api.domain.entity.inputoptimization;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.DomainMultiple;
import com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.DomainSingle;
import com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.DomainStrategy;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@RequiredArgsConstructor
public enum DomainType {
    ABSENCES(new DomainMultiple(), Absences.class),
    BACKLOG_BOUNDS(new DomainMultiple(), BacklogBounds.class),
    CONFIGURATION(new DomainSingle(), Configuration.class),
    CONTRACT_MODALITY_TYPE(new DomainMultiple(), ContractModalityType.class),
    NON_SYSTEMIC_RATIO(new DomainMultiple(), NonSystemicRatio.class),
    POLYVALENCE_PARAMETERS(new DomainMultiple(), PolyvalenceParameters.class),
    PRESENCES(new DomainMultiple(), Presences.class),
    SHIFT_CONTRACT_MODALITY(new DomainMultiple(), ShiftContractModality.class),
    SHIFTS_PARAMETERS(new DomainMultiple(), ShiftParameters.class),
    TRANSFERS(new DomainMultiple(), Transfers.class),
    WORKER_COSTS(new DomainMultiple(), WorkerCosts.class),
    WORKERS_PARAMETERS(new DomainMultiple(), WorkersParameters.class);

    public final DomainStrategy domainStrategy;

    public final Class<? extends Domain> structure;

    private static final Map<String, DomainType> LOOKUP = Arrays.stream(values()).collect(
            toMap(DomainType::toString, Function.identity())
    );

    public static Optional<DomainType> of(final String value) {
        return ofNullable(LOOKUP.get(value.toUpperCase()));
    }

    @JsonValue
    public String toJson() {
        return toString().toLowerCase();
    }

    public interface Domain {
        default boolean conditionFilter(Map<String, List<Object>> domainFilterRequests) {
            return true;
        }
    }

    @Value
    private static class Absences implements Domain {

        String dayName;

        String shiftName;

        String contractModality;

        float unjustifiedAbsentRate;

        float justifiedAbsentRate;

    }

    @Value
    private static class BacklogBounds implements Domain {

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

        boolean hourlyWorkersCost;

    }

    @Value
    private static class ContractModalityType implements Domain {

        String contractModality;

        String contractModalityType;

    }

    @Value
    private static class NonSystemicRatio implements Domain {

        String process;

        String subProcess;

        List<ShiftRatio> shiftRatios;

    }

    @Value
    private static class ShiftRatio {

        String key;

        String value;

    }

    @Value
    private static class PolyvalenceParameters implements Domain {

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
    private static class Presences implements Domain {

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

        String process;

        String shiftName;

        String contractModality;

    }

    @Value
    private static class ShiftParameters implements Domain {

        String dayName;

        String shiftName;

        String shiftType;

        int start;

        int end;

        @Override
        public boolean conditionFilter(Map<String, List<Object>> domainFilterRequests) {

            final Map<String, Function<List<Object>, Boolean>> SHIFT_PARAMETERS_FILTERS = Map.of(
                    "include_day_name", (List<Object> objectList) -> {
                        final List<String> dayNames = objectList.stream()
                                .map(Object::toString)
                                .collect(Collectors.toList());
                        return dayNames.contains(dayName);
                    },
                    "include_shift_type", (List<Object> objectList) -> {
                        final List<String> shiftTypes = objectList.stream()
                                .map(Object::toString)
                                .collect(Collectors.toList());
                        return shiftTypes.contains(shiftType);
                    }
            );

            if (domainFilterRequests == null || domainFilterRequests.isEmpty()) {
                return true;
            }

            return domainFilterRequests.entrySet().stream()
                    .map(filterRequest -> SHIFT_PARAMETERS_FILTERS.get(filterRequest.getKey()).apply(filterRequest.getValue()))
                    .reduce(true, Boolean::logicalAnd);
        }

    }

    @Value
    private static class Transfers implements Domain {

        String shiftNameOrigin;

        String shiftNameDestination;

        float cost;

    }

    @Value
    private static class WorkerCosts implements Domain {

        String shiftName;

        String contractModality;

        float hiringCost;

        float dismissalCost;

        float unitaryCost;

    }

    @Value
    private static class WorkersParameters implements Domain {

        String process;

        String contractModality;

        int stage;

        String shiftName;

        Integer workForce;

        Integer maxWorkers;

    }

}
