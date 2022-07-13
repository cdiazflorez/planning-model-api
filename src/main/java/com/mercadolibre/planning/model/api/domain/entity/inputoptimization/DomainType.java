package com.mercadolibre.planning.model.api.domain.entity.inputoptimization;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@RequiredArgsConstructor
public enum DomainType {
    ABSENCES(Absences[].class),
    BACKLOG_BOUNDS(BacklogBounds[].class),
    CONFIGURATION(Configuration.class),
    CONTRACT_MODALITY_TYPE(ContractModalityType[].class),
    NON_SYSTEMIC_RATIO(NonSystemicRatio[].class),
    POLYVALENCE_PARAMETERS(PolyvalenceParameters[].class),
    PRESENCES(Presences[].class),
    SHIFT_CONTRACT_MODALITY(ShiftContractModality[].class),
    SHIFTS_PARAMETERS(ShiftParameters[].class),
    TRANSFERS(Transfers[].class),
    WORKER_COSTS(WorkerCosts[].class),
    WORKERS_PARAMETERS(WorkersParameters[].class);

    public final Class<?> domainStructure;

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

    @Value
    public static class Absences {

        String dayName;

        String shiftName;

        String contractModality;

        float unjustifiedAbsentRate;

        float justifiedAbsentRate;

    }

    @Value
    public static class BacklogBounds {

        String process;

        int stage;

        String stageName;

        int lowerBound;

        int upperBound;

    }

    @Value
    public static class Configuration {

        boolean anticipateBacklog;

        boolean fixCptsOutbound;

        boolean fixSlasInbound;

        boolean generateValidationFiles;

        boolean activeTransfers;

        boolean activateHourlyWorkers;

        boolean hourlyWorkersCost;

    }

    @Value
    public static class ContractModalityType {

        String contractModality;

        String contractModalityType;

    }

    @Value
    public static class NonSystemicRatio {

        String process;

        String subProcess;

        List<ShiftRatio> shiftRatios;

    }

    @Value
    public static class ShiftRatio {

        String key;

        String value;

    }

    @Value
    public static class PolyvalenceParameters {

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
    public static class Presences {

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
    public static class ShiftContractModality {

        String process;

        String shiftName;

        String contractModality;

    }

    @Value
    public static class ShiftParameters {

        String dayName;

        String shiftName;

        String shiftType;

        int start;

        int end;

    }

    @Value
    public static class Transfers {

        String shiftNameOrigin;

        String shiftNameDestination;

        float cost;

    }

    @Value
    public static class WorkerCosts {

        String shiftName;

        String contractModality;

        float hiringCost;

        float dismissalCost;

        float unitaryCost;

    }

    @Value
    public static class WorkersParameters {

        String process;

        String contractModality;

        int stage;

        String shiftName;

        Integer workForce;

        Integer maxWorkers;

    }

}
