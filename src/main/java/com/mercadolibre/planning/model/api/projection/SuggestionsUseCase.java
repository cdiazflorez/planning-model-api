package com.mercadolibre.planning.model.api.projection;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.TriggerName;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class SuggestionsUseCase {

    public static final int GAP_IN_MINUTES = 5;

    // This constant is temporary until the feature is developed
    public static final int UPPER_BOUND = 15000;

    public List<Suggestion> execute(
            final List<ProcessPathConfiguration> ctByProcessPath,
            final List<UnitsByProcessPathAndProcess> backlogs,
            final Instant viewDate
    ) {
        final var configByProcessPath = ctByProcessPath.stream()
                .collect(Collectors.toMap(ProcessPathConfiguration::getProcessPath, Function.identity()));

        final var inRushPhaseBacklogByPPAndDateOut = backlogs.stream()
                .filter(backlog -> backlog.getProcessName().equals(ProcessName.WAVING))
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

        final List<Wave> executedProcessPaths = inRushPhaseBacklogByPPAndDateOut.entrySet()
                .stream()
                .map(entry -> calculateBoundsByPPForClosenessSla(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        final List<UnitsByDateOut> expectedQuantities = inRushPhaseBacklogByPPAndDateOut.entrySet()
                .stream()
                .flatMap(entry -> entry.getValue()
                        .entrySet()
                        .stream()
                        .map(b -> new UnitsByDateOut(b.getKey(), b.getValue()))
                )
                .collect(Collectors.toList());

        return List.of(new Suggestion(viewDate, executedProcessPaths, TriggerName.SLA, expectedQuantities));
    }

    private Wave calculateBoundsByPPForClosenessSla(
            final ProcessPath processPath,
            final Map<Instant, Integer> backlogBySLA
    ) {
        final int lowerBound = backlogBySLA.values().stream().reduce(0, Integer::sum);
        return new Wave(processPath, lowerBound, UPPER_BOUND, new TreeSet<>(backlogBySLA.keySet()));
    }

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
        final long diffWithNormal = Math.abs(ChronoUnit.MINUTES.between(executionDate, normalCutOff));
        final long diffWithMin = Math.abs(ChronoUnit.MINUTES.between(executionDate, minCutOff));
        return diffWithMin <= GAP_IN_MINUTES || diffWithNormal <= GAP_IN_MINUTES;
    }
}
