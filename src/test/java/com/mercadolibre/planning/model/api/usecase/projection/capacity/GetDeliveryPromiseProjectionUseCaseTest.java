package com.mercadolibre.planning.model.api.usecase.projection.capacity;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.get.GetConfigurationsUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeInput;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.Backlog;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CalculateCptProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.ProcessingTime;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.GetDeliveryPromiseProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetDeliveryPromiseProjectionInput;
import com.mercadolibre.planning.model.api.usecase.ProcessingDistributionViewImpl;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.api.util.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.ProjectionType.DEFERRAL;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SuppressWarnings("PMD.ExcessiveImports")
@ExtendWith(MockitoExtension.class)
public class GetDeliveryPromiseProjectionUseCaseTest {

    private static final ZonedDateTime CPT_1 = ZonedDateTime.now().plusHours(1);
    private static final ZonedDateTime CPT_2 = ZonedDateTime.now().plusHours(2);
    private static final ZonedDateTime NOW = ZonedDateTime.now(UTC);

    @InjectMocks
    private GetDeliveryPromiseProjectionUseCase useCase;

    @Mock
    private CalculateCptProjectionUseCase projectionUseCase;

    @Mock
    private ProcessingDistributionRepository processingDistRepository;

    @Mock
    private GetForecastUseCase getForecastUseCase;

    @Mock
    private GetConfigurationsUseCase getConfigurationUseCase;

    @Mock
    private GetCycleTimeUseCase getCycleTimeUseCase;

    @ParameterizedTest
    @MethodSource("getDataMock")
    public void testExecute(final String warehouseId,
                            final List<CptProjectionOutput> projectionOutputs) {
        //GIVEN
        final GetDeliveryPromiseProjectionInput input = GetDeliveryPromiseProjectionInput.builder()
                .warehouseId(warehouseId)
                .workflow(Workflow.FBM_WMS_OUTBOUND)
                .dateFrom(NOW)
                .dateTo(NOW.plusHours(6))
                .backlog(List.of(new Backlog(CPT_1, 100), new Backlog(CPT_2, 200)))
                .build();

        final List<Long> forecastIds = List.of(1L, 2L);

        when(getForecastUseCase.execute(GetForecastInput.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getWarehouseId())
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .build())).thenReturn(forecastIds);

        when(processingDistRepository
                .findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                        Set.of(ProcessingType.MAX_CAPACITY.name()),
                        List.of(ProcessName.GLOBAL.toJson()),
                        input.getDateFrom(),
                        input.getDateTo(),
                        forecastIds
                )).thenReturn(mockProcessingDist());

        when(projectionUseCase.execute(CptProjectionInput.builder()
                .workflow(input.getWorkflow())
                .logisticCenterId(input.getWarehouseId())
                .capacity(mockCapacityByHour())
                .backlog(input.getBacklog())
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .planningUnits(Collections.emptyList())
                .projectionType(DEFERRAL)
                .currentDate(getCurrentUtcDate())
                .build())
        ).thenReturn(projectionOutputs);

        final Configuration cycleTimeConfig = Configuration.builder()
                .value(360L)
                .metricUnit(MINUTES)
                .key("cycle_time_16_00")
                .build();

        when(getConfigurationUseCase.execute(input.getWarehouseId()))
                .thenReturn(List.of(
                        cycleTimeConfig,
                        Configuration.builder()
                                .value(360L)
                                .metricUnit(MINUTES)
                                .key("processing_time")
                                .build()
                ));

        when(getCycleTimeUseCase.execute(any(GetCycleTimeInput.class)))
                .thenReturn(cycleTimeConfig)
                .thenReturn(cycleTimeConfig);

        //WHEN
        final List<CptProjectionOutput> response = useCase.execute(input);

        //THEN
        assertEquals(projectionOutputs, response);
    }

    private List<ProcessingDistributionView> mockProcessingDist() {
        return List.of(
                ProcessingDistributionViewImpl.builder()
                        .date(Date.from(NOW.toLocalDateTime().plusHours(1).toInstant(UTC)))
                        .quantity(120L)
                        .build(),
                ProcessingDistributionViewImpl.builder()
                        .date(Date.from(NOW.toLocalDateTime().plusHours(2).toInstant(UTC)))
                        .quantity(100L)
                        .build(),
                ProcessingDistributionViewImpl.builder()
                        .date(Date.from(NOW.toLocalDateTime().plusHours(3).toInstant(UTC)))
                        .quantity(130L)
                        .build(),
                ProcessingDistributionViewImpl.builder()
                        .date(Date.from(NOW.toLocalDateTime().plusHours(5).toInstant(UTC)))
                        .quantity(100L)
                        .build()
        );
    }

    private Map<ZonedDateTime, Integer> mockCapacityByHour() {
        final Map<ZonedDateTime, Integer> map = new TreeMap<>();
        map.put(NOW.truncatedTo(SECONDS), 130);
        map.put(NOW.plusHours(1).truncatedTo(SECONDS), 120);
        map.put(NOW.plusHours(2).truncatedTo(SECONDS), 100);
        map.put(NOW.plusHours(3).truncatedTo(SECONDS), 130);
        map.put(NOW.plusHours(4).truncatedTo(SECONDS), 130);
        map.put(NOW.plusHours(5).truncatedTo(SECONDS), 100);

        return map;

    }

    public static Stream<Arguments> getDataMock() {
        return Stream.of(
                Arguments.of(
                        "ARBA01",
                        List.of(
                                CptProjectionOutput.builder()
                                        .date(CPT_1)
                                        .projectedEndDate(null)
                                        .remainingQuantity(0)
                                        .processingTime(new ProcessingTime(360L, MINUTES))
                                        .isDeferred(false)
                                        .build(),
                                CptProjectionOutput.builder()
                                        .date(CPT_2)
                                        .projectedEndDate(null)
                                        .remainingQuantity(0)
                                        .processingTime(new ProcessingTime(360L, MINUTES))
                                        .isDeferred(false)
                                        .build()
                        )
                )
        );
    }
}
