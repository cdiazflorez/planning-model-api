package com.mercadolibre.planning.model.api.domain.entity.inputoptimization;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.domain.DomainShiftParameter;
import com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.domain.DomainConfiguration;
import com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.domain.DomainStrategy;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@RequiredArgsConstructor
public enum DomainType {
    ABSENCES(null),
    BACKLOG_BOUNDS(null),
    CONFIGURATION(null),
    CONTRACT_MODALITY_TYPE(null),
    NON_SYSTEMIC_RATIO(null),
    POLYVALENCE_PARAMETERS(null),
    PRESENCES(null),
    SHIFT_CONTRACT_MODALITY(null),
    SHIFTS_PARAMETERS(new DomainShiftParameter()),
    TRANSFERS(null),
    WORKER_COSTS(null),
    WORKERS_PARAMETERS(null);

    public final DomainStrategy domainStrategy;

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
        default Map<String, Function<List<String>, Predicate<Domain>>> getDomainPredicate() {
            return Map.of();
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
    public static class ShiftParameters implements Domain {

        String dayName;

        String shiftName;

        String shiftType;

        int start;

        int end;

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
