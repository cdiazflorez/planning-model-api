package com.mercadolibre.planning.model.api.projection.builder;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mercadolibre.flow.projection.tools.services.entities.context.ContextsHolder;
import com.mercadolibre.flow.projection.tools.services.entities.context.PiecewiseUpstream;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.OrderedBacklogByDate;
import com.mercadolibre.flow.projection.tools.services.entities.process.ParallelProcess;
import com.mercadolibre.flow.projection.tools.services.entities.process.Processor;
import com.mercadolibre.flow.projection.tools.services.entities.process.SequentialProcess;
import com.mercadolibre.flow.projection.tools.services.entities.process.SimpleProcess;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.backlogmanager.OrderedBacklogByProcessPath;
import com.mercadolibre.planning.model.api.util.DateUtils;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class FromWavingToPackingProjectionBuilderTest {

    private static final String PRE_EXPEDITION_GROUP = "pre_expedition_group";
    private static final Instant FIRST_DATE = Instant.parse("2022-10-03T00:00:00.00Z");
    private static final Instant LAST_DATE = Instant.parse("2022-10-03T02:00:00.00Z");
    private static final Instant[] DATES = {
            Instant.parse("2023-03-29T00:00:00Z"),
            Instant.parse("2023-03-29T01:00:00Z"),
            Instant.parse("2023-03-29T02:00:00Z"),
            Instant.parse("2023-03-29T03:00:00Z"),
            Instant.parse("2023-03-29T04:00:00Z"),
            Instant.parse("2023-03-29T05:00:00Z"),
            Instant.parse("2023-03-29T06:00:00Z"),
    };

    private static final Instant[] SLAS = {
            Instant.parse("2023-03-29T10:00:00Z"),
            Instant.parse("2023-03-29T11:00:00Z"),
            Instant.parse("2023-03-29T12:00:00Z"),
            Instant.parse("2023-03-29T13:00:00Z"),
            Instant.parse("2023-03-29T14:00:00Z"),
    };

    private static final Map<Instant, Instant> CUT_OFF = Map.of(SLAS[0], DATES[0], SLAS[1], DATES[2]);

    private static final Instant[] SLAS_FROM_WAVING = {
            Instant.parse("2023-03-29T10:00:00Z"),
            Instant.parse("2023-03-29T12:00:00Z"),
            Instant.parse("2023-03-29T13:00:00Z"),
    };

    private static final Map<Instant, Instant> EXPECTED_END_DATES_WITH_UPSTREAM = Map.of(
            SLAS_FROM_WAVING[0], Instant.parse("2023-03-29T05:25:00Z"),
            SLAS_FROM_WAVING[1], Instant.parse("2023-03-29T05:06:00Z")
    );

    private static final Map<Instant, Map<ProcessPath, Map<Instant, Long>>> FORECAST = Map.of(
            DATES[0], Map.of(
                    TOT_MONO, Map.of(
                            SLAS[0], 240L
                    )
            ),
            DATES[1], Map.of(
                    TOT_MULTI_BATCH, Map.of(
                            SLAS[1], 1200L
                    )
            ),
            DATES[5], Map.of(
                    NON_TOT_MONO, Map.of(
                            SLAS[4], 1800L
                    )
            )
    );

    private static final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> BACKLOG = Map.of(
            WAVING, Map.of(
                    TOT_MONO, Map.of(SLAS[0], 1000L, SLAS[1], 1000L),
                    NON_TOT_MONO, Map.of(SLAS[0], 250L),
                    TOT_MULTI_BATCH, Map.of(SLAS[1], 500L)
            ),
            PICKING, Map.of(
                    TOT_MONO, Map.of(SLAS[0], 1000L, SLAS[1], 1000L),
                    NON_TOT_MONO, Map.of(SLAS[0], 250L),
                    TOT_MULTI_BATCH, Map.of(SLAS[1], 500L)
            ),
            PACKING, Map.of(
                    TOT_MONO, Map.of(SLAS[0], 1000L, SLAS[2], 500L),
                    NON_TOT_MONO, Map.of(SLAS[0], 100L)
            ),
            PACKING_WALL, Map.of(
                    TOT_MONO, Map.of(SLAS[0], 1000L, SLAS[2], 500L),
                    NON_TOT_MONO, Map.of(SLAS[0], 100L)
            ),
            BATCH_SORTER, Map.of(
                    TOT_MULTI_BATCH, Map.of(SLAS[0], 1000L, SLAS[2], 500L)
            )
    );

    public static Map<Instant, Integer> throughput(int v1, int v2, int v3, int v4, int v5, int v6, int v7) {
        return Map.of(
                DATES[0], v1,
                DATES[1], v2,
                DATES[2], v3,
                DATES[3], v4,
                DATES[4], v5,
                DATES[5], v6,
                DATES[6], v7
        );
    }

    @Test
    void testProjectionStepCompatibility() {
        final var builder = new FromWavingToPackingProjectionBuilder();

        final List<Instant> inflectionPoints = DateUtils.generateInflectionPoints(FIRST_DATE, LAST_DATE, 1);

        final PiecewiseUpstream upstream = builder.toUpstream(FORECAST);

        final ContextsHolder initialContext = builder.buildContextHolder(BACKLOG, createThroughputTestData());

        final Processor graph = builder.buildGraph();

        final var contextAfterFirstStep = graph.accept(initialContext, upstream, inflectionPoints);

        contextAfterFirstStep.getProcessContextByProcessName()
                .forEach((processName, processContext) -> assertFalse(processContext.getProcessedBacklog().isEmpty()));
        assertNotNull(contextAfterFirstStep);

        final var contextAfterSecondStep = graph.accept(contextAfterFirstStep, upstream, List.of());
        assertNotNull(contextAfterSecondStep);
    }

    @Test
    void testToUpstream() {
        final FromWavingToPackingProjectionBuilder fromWavingToPackingProjectionBuilder = new FromWavingToPackingProjectionBuilder();
        final PiecewiseUpstream piecewiseUpstream = fromWavingToPackingProjectionBuilder.toUpstream(FORECAST);

        final var expected = Optional.of(new OrderedBacklogByProcessPath(
                Collections.singletonMap(
                        TOT_MONO,
                        new OrderedBacklogByDate(Collections.singletonMap(
                                SLAS[0],
                                new OrderedBacklogByDate.Quantity(240)
                        ))
                )
        ));

        assertEquals(expected, piecewiseUpstream.calculateUpstreamUnitsForInterval(DATES[0], DATES[1]));
        assertEquals(7, piecewiseUpstream.getUpstreamByPiece().size());
    }

    @Test
    void testBuildGraph() {
        final FromWavingToPackingProjectionBuilder fromWavingToPackingProjectionBuilder = new FromWavingToPackingProjectionBuilder();
        final Processor graph = fromWavingToPackingProjectionBuilder.buildGraph();

        assertPreExpeditionGroup(graph);
    }

    @Test
    void testBuildContextHolder() {
        final FromWavingToPackingProjectionBuilder fromWavingToPackingProjectionBuilder = new FromWavingToPackingProjectionBuilder();

        final Map<ProcessName, Map<Instant, Integer>> throughput = createThroughputTestData();

        final ContextsHolder contextsHolder = fromWavingToPackingProjectionBuilder.buildContextHolder(BACKLOG, throughput);

        assertContextsHolder(contextsHolder);
    }

    @Test
    void testBacklogProjection() {
        // GIVEN
        final var builder = new FromWavingToPackingProjectionBuilder();

        final PiecewiseUpstream upstream = builder.toUpstream(FORECAST);

        final ContextsHolder holder = builder.buildContextHolder(BACKLOG, createThroughputTestData());

        final Processor graph = builder.buildGraph();

        final List<Instant> inflectionPoints = DateUtils.generateInflectionPoints(DATES[0], DATES[6], 5);

        final var updatedContext = graph.accept(holder, upstream, inflectionPoints);

        // WHEN
        final var result = builder.calculateProjectedEndDate(Arrays.asList(SLAS), updatedContext);

        // THEN
        assertNotNull(result);

        final var projectedEndDateBySla = result.slas()
                .stream()
                .filter(r -> r.projectedEndDate() != null)
                .collect(
                        Collectors.toMap(
                                SlaProjectionResult.Sla::date,
                                r -> r.projectedEndDate().truncatedTo(ChronoUnit.MINUTES)
                        )
                );


        for (final Instant sla : SLAS) {
            assertEquals(EXPECTED_END_DATES_WITH_UPSTREAM.get(sla), projectedEndDateBySla.get(sla));
        }
    }

    @Test
    void testGetRemainingQuantity() {
        // GIVEN
        final FromWavingToPackingProjectionBuilder builder = new FromWavingToPackingProjectionBuilder();

        final PiecewiseUpstream upstream = builder.toUpstream(FORECAST);

        final ContextsHolder initialContext = builder.buildContextHolder(BACKLOG, createThroughputTestData());

        final Processor graph = builder.buildGraph();

        final List<Instant> inflectionPoints = DateUtils.generateInflectionPoints(DATES[0], DATES[6], 5);

        final ContextsHolder updatedContext = graph.accept(initialContext, upstream, inflectionPoints);

        // WHEN
        final Map<Instant, Long> remainingQuantity = builder.getRemainingQuantity(updatedContext, CUT_OFF);

        // THEN
        assertNotNull(remainingQuantity);
        assertFalse(remainingQuantity.isEmpty());

        assertEquals(6820, remainingQuantity.get(SLAS[0]));
        assertEquals(4561, remainingQuantity.get(SLAS[1]));
    }

    private void assertPreExpeditionGroup(final Processor graph) {
        assertInstanceOf(SequentialProcess.class, graph);
        final SequentialProcess sequentialProcess = (SequentialProcess) graph;
        assertEquals(PRE_EXPEDITION_GROUP, sequentialProcess.getName());
        assertEquals(2, sequentialProcess.getProcesses().size());

        assertWavingProcess((SimpleProcess) sequentialProcess.getProcesses().get(0));
        assertOrderAssemblyProcess((SequentialProcess) sequentialProcess.getProcesses().get(1));

    }

    private void assertWavingProcess(final SimpleProcess wavingProcess) {
        assertNotNull(wavingProcess);
        assertEquals(WAVING.getName(), wavingProcess.getName());
    }

    private void assertOrderAssemblyProcess(final SequentialProcess orderAssemblyProcess) {
        assertNotNull(orderAssemblyProcess);
        assertEquals("order_assembly", orderAssemblyProcess.getName());
        assertEquals(2, orderAssemblyProcess.getProcesses().size());

        assertPickingProcess((SimpleProcess) orderAssemblyProcess.getProcesses().get(0));
        assertPackingGroupProcess((ParallelProcess) orderAssemblyProcess.getProcesses().get(1));
    }

    private void assertPickingProcess(final SimpleProcess pickingProcess) {
        assertNotNull(pickingProcess);
        assertEquals(PICKING.getName(), pickingProcess.getName());
    }

    private void assertPackingGroupProcess(final ParallelProcess packingGroupProcess) {
        assertNotNull(packingGroupProcess);
        assertEquals("packing_group", packingGroupProcess.getName());
        assertEquals(2, packingGroupProcess.getProcessors().size());

        assertPackingProcess((SimpleProcess) packingGroupProcess.getProcessors().get(0));
    }

    private void assertPackingProcess(final SimpleProcess packingProcess) {
        assertNotNull(packingProcess);
        assertEquals(PACKING.getName(), packingProcess.getName());
    }

    private Map<ProcessName, Map<Instant, Integer>> createThroughputTestData() {
        return Map.of(
                WAVING, throughput(1000, 1000, 1000, 1000, 1000, 1000, 1000),
                PICKING, throughput(1000, 1000, 1000, 1000, 1000, 1000, 1000),
                PACKING, throughput(1000, 1000, 1000, 1000, 1000, 1000, 1000),
                BATCH_SORTER, throughput(1000, 1000, 1000, 1000, 1000, 1000, 1000),
                WALL_IN, throughput(1000, 1000, 1000, 1000, 1000, 1000, 1000),
                PACKING_WALL, throughput(1000, 1000, 1000, 1000, 1000, 1000, 1000)
        );
    }

    private void assertContextsHolder(final ContextsHolder contextsHolder) {
        assertNotNull(contextsHolder);

        for (ProcessName process : List.of(WAVING, PACKING, BATCH_SORTER, WALL_IN, PACKING_WALL)) {
            assertNotNull(contextsHolder.getProcessContextByProcessName(process.getName()));
            final var result = (SimpleProcess.Context) contextsHolder.getProcessContextByProcessName(process.getName());
            assertEquals(1000, result.getTph().availableBetween(DATES[0], DATES[1]));
        }

    }
}
