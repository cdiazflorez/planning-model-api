package com.mercadolibre.planning.model.api.domain.entity;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class WorkflowTest {

    private static Stream<Arguments> inbound() {
        return Stream.of(
                arguments(Map.of(), 0L),
                arguments(Map.of(PUT_AWAY, 10L), 10L)
        );
    }

    private static Stream<Arguments> outbound() {
        return Stream.of(
                arguments(Map.of(), 0L),
                arguments(Map.of(PICKING, 10L), 0L),
                arguments(Map.of(PACKING, 10L), 0L),
                arguments(Map.of(PACKING_WALL, 10L), 0L),
                arguments(
                        Map.of(
                                PICKING, 10L,
                                PACKING, 6L,
                                PACKING_WALL, 5L),
                        10L),
                arguments(
                        Map.of(
                                PICKING, 10L,
                                PACKING, 5L,
                                PACKING_WALL, 6L),
                        10L)
        );
    }

    @ParameterizedTest
    @MethodSource("inbound")
    void testInboundCapacity(final Map<ProcessName, Long> capacities, final Long expected) {
        final var result = FBM_WMS_INBOUND.getCapacityCalculator().apply(capacities);
        assertEquals(expected, result);
    }

    @ParameterizedTest
    @MethodSource("outbound")
    void testOutboundCapacity(final Map<ProcessName, Long> capacities, final Long expected) {
        final var result = FBM_WMS_OUTBOUND.getCapacityCalculator().apply(capacities);
        assertEquals(expected, result);
    }
}
