package com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;

public final class QueueProjectionCalculator {

    private QueueProjectionCalculator() {
    }

    /**
     * Calculates the length of the processing queue of a stage will be at different times in the future.
     *
     * @param startingDate       the projection starting date
     * @param startingQueue      the {@link Queue} at the projection starting date.
     * @param inflectionInstants the instants for which que projected {@link Queue} should be calculated.
     * @param assistant          an instance of the {@link Assistant} interface that tells what this stage needs to know about the
     *                           upstream stage and this stage processing power.
     * @param <D>                the quantity discriminator type whose ordering criteria determines this stage's queue processing order.
     * @return a record at each of the inflection instant that contains the queue at that instant and the exhaustion date of each pile
     * that was exhausted during the step associated to the record.
     */
    public static <D extends Comparable<D>> Log<D> calculate(
            final Instant startingDate,
            final Queue<D> startingQueue,
            final NavigableSet<Instant> inflectionInstants,
            final Assistant<D> assistant
    ) {
        final var recordByStepEndingDate = new TreeMap<Instant, Record<D>>();
        var record = new Record<>(startingQueue, new TreeMap<>(), 0);
        recordByStepEndingDate.put(startingDate, record);
        var stepStartingDate = startingDate;
        for (final var stepEndingDate : inflectionInstants.tailSet(startingDate, false)) {
            final var stepStartingDateFinal = stepStartingDate;
            record = record.queueAtEndOfStep
                    .merged(assistant.calcUpstreamIntegral(stepStartingDate, stepEndingDate))
                    .consumed(
                            assistant.calcProcessingCapacityIntegral(stepStartingDate, stepEndingDate),
                            quantity -> assistant.calcExhaustionDate(stepStartingDateFinal, quantity)
                    );
            recordByStepEndingDate.put(stepEndingDate, record);
            stepStartingDate = stepEndingDate;
        }
        return new Log<>(recordByStepEndingDate);
    }

    /**
     * Specifies the assistance that the {@link #calculate(Instant, Queue, NavigableSet, Assistant)}
     * method requires knowing about the upstream stage and this stage processing power.
     */
    public interface Assistant<D extends Comparable<D>> {
        /**
         * The implementation should give the output of the upstream stage between the specified instants.
         * The implementation may assume that no inflection instant is between the received boundaries.
         *
         * @param from the integral's starting instant
         * @param to   the integral's ending instant
         * @return the output of the upstream stage between the specified instant.
         * */
        Queue<D> calcUpstreamIntegral(Instant from, Instant to);

        /**
         * The implementation should give the consumption power of this stage between the specified instants.
         * The implementation may assume that no inflection instant is between the received boundaries
         *
         * @param from the integral's starting instant
         * @param to   the integral's ending instant
         * @return the processing capacity between two instants of time
         */
        long calcProcessingCapacityIntegral(Instant from, Instant to);

        /**
         * The implementation should give the instant at which the received quantity of elements would be completely exhausted if the
         * consumption starts at the specified instant.
         * The implementation may assume that the resulting exhaustion date is before or equal to the first inflection instant immediately
         * after the specified date.
         *
         * @param startingDate      the instant at which the consumption starts.
         * @param quantityToProcess the number of elements we want to know how long it takes to consume.
         * @return the instant at which the received quantity of elements would be completely exhausted if the
         * consumption starts at the specified instant
         */
        Instant calcExhaustionDate(Instant startingDate, long quantityToProcess);
    }

    /**
     * A queue that prioritizes the consumption of elements according to the discriminator they are associated with.
     * Elements with same discriminator are fungible.
     * @param <D> the discriminator type.
     */
    @RequiredArgsConstructor
    public static class Queue<D extends Comparable<D>> {

        private final NavigableMap<D, Long> quantityByDiscriminator;

        public NavigableMap<D, Long> getQuantityByDiscriminator() {
            return Collections.unmodifiableNavigableMap(this.quantityByDiscriminator);
        }

        /**
         * Gives a {@link Record} that contains:
         * - a new {@link Queue} that results of consuming the specified quantity of elements from this instance;
         * - for all the groups of this queue that were consumed completely, the instant when that happened and;
         * - the difference between the received quantity and the total number of elements in this queue.
         * @param quantity quantity of units to consume
         * @param consumptionDateCalculator calculates when is the instant in which the elements will be consumed
         * @return a record with the result of elements
         */
        public Record<D> consumed(final long quantity, final Function<Long, Instant> consumptionDateCalculator) {
            final TreeMap<D, Instant> exhaustionInstantByDiscriminator = new TreeMap<>();
            long remaining = quantity;
            final var accumulator = new TreeMap<D, Long>();
            for (var entry : this.quantityByDiscriminator.entrySet()) {
                if (entry.getValue() <= remaining) {
                    remaining -= entry.getValue();
                    if (entry.getValue() > 0) {
                        exhaustionInstantByDiscriminator.put(entry.getKey(), consumptionDateCalculator.apply(quantity - remaining));
                    }
                } else {
                    accumulator.put(entry.getKey(), entry.getValue() - remaining);
                    remaining = 0;
                }
            }
            return new Record<>(new Queue<>(accumulator), exhaustionInstantByDiscriminator, remaining);
        }

        /**
         * @param other queue to merge with the actual queue.
         * @return a new {@link Queue} that results of merging this and the received {@link Queue}s.
         */
        public Queue<D> merged(final Queue<D> other) {
            var newQuantityByDiscriminator = Stream.concat(
                    this.quantityByDiscriminator.entrySet().stream(),
                    other.quantityByDiscriminator.entrySet().stream()
            ).collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    Long::sum,
                    TreeMap::new
            ));
            return new Queue<>(newQuantityByDiscriminator);
        }
    }

    @RequiredArgsConstructor
    public static class Record<D extends Comparable<D>> {
        final Queue<D> queueAtEndOfStep;

        final NavigableMap<D, Instant> exhaustionInstantByDiscriminator;

        final long overconsumption;
    }

    @RequiredArgsConstructor
    public static class Log<D extends Comparable<D>> {
        final NavigableMap<Instant, Record<D>> recordByStepEndingDate;
    }
}
