package com.mercadolibre.planning.model.api.projection;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toUnmodifiableList;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessNameToProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.TriggerName;
import com.mercadolibre.planning.model.api.exception.InvalidArgumentException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class SuggestionsUseCase {

    public static final int GAP_IN_MINUTES = 5;

    public List<Suggestion> execute(
            final List<ProcessPathConfiguration> ctByProcessPath,
            final List<UnitsByProcessPathAndProcess> backlogs,
            final Map<ProcessPath, Map<Instant, Float>> ratiosByProcessPath,
            final Map<ProcessName, Map<Instant, Integer>> backlogLimitsByHourOfOperationAndProcess,
            final Instant viewDate
    ) {

        final List<UnitsByProcessPathAndProcess> wavingBacklog = backlogs.stream()
                .filter(unitsByProcessPathAndProcess -> unitsByProcessPathAndProcess.getProcessName().equals(ProcessName.WAVING))
                .collect(toUnmodifiableList());

        final List<UnitsByProcessPathAndProcess> filteredBacklog = backlogs.stream()
                .filter(unitsByProcessPathAndProcess -> !unitsByProcessPathAndProcess.getProcessName().equals(ProcessName.WAVING))
                .collect(toUnmodifiableList());

        final Map<ProcessName, Integer> backlogByProcess = filteredBacklog.stream()
                .collect(
                        toMap(
                                UnitsByProcessPathAndProcess::getProcessName,
                                UnitsByProcessPathAndProcess::getUnits,
                                Integer::sum
                        ));

        final Instant operationHour = viewDate.truncatedTo(ChronoUnit.HOURS);

        final Map<ProcessName, Integer> backlogLimitsByProcess = backlogLimitsByHourOfOperationAndProcess.entrySet().stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().get(operationHour)
                ));

        final Map<ProcessName, Integer> totalCapacityByProcessName = backlogLimitsByProcess.entrySet()
                .stream()
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> Optional.ofNullable(backlogByProcess.get(entry.getKey()))
                                        .map(b -> Math.max(entry.getValue() - b, 0))
                                        .orElse(entry.getValue())
                        )
                );

        final Map<ProcessPath, Float> ratios = ratiosByProcessPath.entrySet().stream()
                .collect(
                        toMap(
                                Map.Entry::getKey,
                                processPathMapEntry -> processPathMapEntry.getValue().get(operationHour)
                        )
                );

        final Map<ProcessPath, Float> upperBoundsByProcessPath = calculateUpperBoundsForSlaCloseness(ratios, totalCapacityByProcessName);

        final Map<ProcessPath, ProcessPathConfiguration> configByProcessPath = ctByProcessPath.stream()
                .collect(Collectors.toMap(ProcessPathConfiguration::getProcessPath, Function.identity()));

        final Map<ProcessPath, Map<Instant, Integer>> inRushPhaseBacklogByPPAndDateOut = wavingBacklog.stream()
                .filter(backlog -> shouldTriggerRushPhaseWave(
                        backlog.getDateOut(),
                        viewDate,
                        configByProcessPath.get(backlog.getProcessPath())
                ))
                .collect(groupingBy(
                        UnitsByProcessPathAndProcess::getProcessPath,
                        groupingBy(
                                UnitsByProcessPathAndProcess::getDateOut,
                                reducing(0, UnitsByProcessPathAndProcess::getUnits, Integer::sum)
                        )
                ));

        final List<Wave> waves = inRushPhaseBacklogByPPAndDateOut.entrySet()
                .stream()
                .map(entry -> buildSuggestionForClosenessSla(
                        entry.getKey(),
                        upperBoundsByProcessPath.get(entry.getKey()).intValue(),
                        entry.getValue())
                )
                .sorted((Comparator.comparing(x -> x.getProcessPath().name())))
                .collect(Collectors.toList());

        final List<UnitsByDateOut> expectedQuantities = inRushPhaseBacklogByPPAndDateOut.entrySet()
                .stream()
                .flatMap(entry -> entry.getValue()
                        .entrySet()
                        .stream()
                        .map(b -> new UnitsByDateOut(b.getKey(), b.getValue()))
                )
                .sorted(Comparator.comparing(UnitsByDateOut::getDateOut))
                .collect(Collectors.toList());

        return List.of(
                new Suggestion(viewDate, waves, TriggerName.SLA, expectedQuantities)
        );
    }

    /**
     * Builds a suggestion with the received parameters
     * Note that if the lowerBound is greater than the {@code upperBound} it will remain the same as the {@code upperBound}
     * @param processPath process path in which want to create the suggestion
     * @param upperBound that will have the suggestion
     * @param backlogBySLA with which the lowerBound will be calculated
     * @return a {@link Wave} with which the suggestion will be created
     */
    private Wave buildSuggestionForClosenessSla(
            final ProcessPath processPath,
            final int upperBound,
            final Map<Instant, Integer> backlogBySLA
    ) {
        final int sumOfUnits = backlogBySLA.values().stream().reduce(0, Integer::sum);
        final int lowerBound = Math.min(sumOfUnits, upperBound);
        return new Wave(processPath, lowerBound, upperBound, new TreeSet<>(backlogBySLA.keySet()));
    }

    /**
     * Decide whether if it is in the rush phase
     * @param dateOut the SLA time
     * @param executionDate time the decision is being calculated
     * @param conf contains for each process path the parameters to calculate the cut-offs
     * @return true if it is within the rush phase
     */
    private boolean shouldTriggerRushPhaseWave(
            final Instant dateOut,
            final Instant executionDate,
            final ProcessPathConfiguration conf
    ) {
        if (conf == null || dateOut.isBefore(executionDate)) {
            return false;
        }
        final Instant normalCutOff = dateOut.minus(conf.getNormalCycleTime(), ChronoUnit.MINUTES);
        final Instant minCutOff = dateOut.minus(conf.getMinCycleTime(), ChronoUnit.MINUTES);
        final long diffWithNormal = ChronoUnit.MINUTES.between(executionDate, normalCutOff);
        final long diffWithMin = ChronoUnit.MINUTES.between(executionDate, minCutOff);
        return (diffWithMin >= 0 && diffWithMin <= GAP_IN_MINUTES) || (diffWithNormal >= 0 && diffWithNormal <= GAP_IN_MINUTES);
    }

    private Map<ProcessPath, Float> calculateUpperBoundsForSlaCloseness(
            final Map<ProcessPath, Float> pickingRatios,
            final Map<ProcessName, Integer> backlogLimits
    ) {
        Map<ProcessName, Map<ProcessPath, Float>> processPathAndUnitsByProcessName = new EnumMap<>(ProcessName.class);
        final Integer pickingUpperBound = backlogLimits.get(ProcessName.PICKING);
        final Map<ProcessPath, Float> pickingBounds = pickingRatios.entrySet().stream().collect(toMap(
                Map.Entry::getKey,
                entry -> entry.getValue() * pickingUpperBound
        ));
        processPathAndUnitsByProcessName.put(ProcessName.PICKING, pickingBounds);

        for (ProcessNameToProcessPath processNameToProcessPath : ProcessNameToProcessPath.getTriggersProcess()) {
            final ProcessName process = ProcessName.of(processNameToProcessPath.getName()).orElseThrow();
            final ProcessName previousProcess = process.getPreviousProcesses();
            final Map<ProcessPath, Float> previousProcessContext = processPathAndUnitsByProcessName.get(previousProcess);

            if (previousProcessContext == null) {
                continue;
            }
            final float total = processNameToProcessPath.getPaths().stream()
                    .map(previousProcessContext::get)
                    .filter(Objects::nonNull)
                    .reduce(0F, Float::sum);

            final Integer processPathAvailableCapacity = Optional.ofNullable(backlogLimits.get(process))
                    .orElseThrow(
                            () -> new InvalidArgumentException("backlog limits were not received for process: " + process.getName())
                    );

            final Map<ProcessPath, Float> quantityPreviousProcessPath = processNameToProcessPath.getPaths().stream()
                    .collect(
                            toMap(
                                    Function.identity(),
                                    processPath1 -> (previousProcessContext.get(processPath1) / total) * processPathAvailableCapacity)
                    );

            processPathAndUnitsByProcessName.put(process, quantityPreviousProcessPath);
        }

        return processPathAndUnitsByProcessName.values().stream()
                .map(Map::entrySet)
                .flatMap(Set::stream)
                .collect(
                        toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                Float::min)
                );
    }
}
