package com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt;

import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.QueueProjectionCalculator.Assistant;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AssistantImpl implements Assistant<Instant> {

    private static final long SECONDS_IN_AN_HOUR = TimeUnit.HOURS.toSeconds(1);

    /**
     * the upstream quantity is the forecast sells or the scheduled shipments depending on the workflow is outbound or inbound respectively
     */
    final NavigableMap<Instant, NavigableMap<Instant, Long>> upstreamQuantityByDateOutByDateIn;

    final NavigableMap<Instant, Integer> capacity;

    final Function<Instant, Instant> dateOutToDeadlineMapper;

    @Override
    public QueueProjectionCalculator.Queue<Instant> calcUpstreamIntegral(Instant from, Instant to) {
        final var upstreamQuantityByDateOut = upstreamQuantityByDateOutByDateIn.floorEntry(from);
        final var wholeHourQuantityByDateOut =
                (upstreamQuantityByDateOut == null)
                        ? new TreeMap<Instant, Long>()
                        : upstreamQuantityByDateOutByDateIn.floorEntry(from).getValue();

        final var quantityByCutoff = wholeHourQuantityByDateOut.entrySet().stream()
                .collect(toMap(
                        entry -> dateOutToDeadlineMapper.apply(entry.getKey()),
                        entry -> (entry.getValue() * ChronoUnit.SECONDS.between(from, to)) / SECONDS_IN_AN_HOUR,
                        Long::sum,
                        TreeMap::new
                ));
        return new QueueProjectionCalculator.Queue<>(quantityByCutoff);
    }

    @Override
    public long calcProcessingCapacityIntegral(Instant from, Instant to) {
        var floorEntry = capacity.floorEntry(from);
        if (floorEntry == null) {
            return 0;
        } else {
            final var wholeHourPower = floorEntry.getValue();
            return (wholeHourPower * ChronoUnit.SECONDS.between(from, to)) / SECONDS_IN_AN_HOUR;
        }
    }

    @Override
    public Instant calcExhaustionDate(Instant startingDate, long quantityToProcess) {
        final var wholeHourPower = capacity.floorEntry(startingDate).getValue();
        return startingDate.plusSeconds((SECONDS_IN_AN_HOUR * quantityToProcess) / wholeHourPower);
    }
}
