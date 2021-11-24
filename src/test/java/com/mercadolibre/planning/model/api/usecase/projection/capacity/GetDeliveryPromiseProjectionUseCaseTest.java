package com.mercadolibre.planning.model.api.usecase.projection.capacity;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.usecase.cptbywarehouse.GetCptByWarehouseInput;
import com.mercadolibre.planning.model.api.domain.usecase.cptbywarehouse.GetCptByWarehouseOutput;
import com.mercadolibre.planning.model.api.domain.usecase.cptbywarehouse.GetCptByWarehouseUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.cptbywarehouse.ProcessingTime;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeInput;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.Backlog;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CalculateCptProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptCalculationOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.DeliveryPromiseProjectionOutput;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.api.util.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.ProjectionType.DEFERRAL;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@SuppressWarnings("PMD.ExcessiveImports")
@ExtendWith(MockitoExtension.class)
public class GetDeliveryPromiseProjectionUseCaseTest {

    private static final String WAREHOUSE_ID = "ARBA01";
    private static final ZonedDateTime NOW = ZonedDateTime.now(UTC);
    private static final ZonedDateTime CPT_1 = NOW.plusHours(1);
    private static final ZonedDateTime CPT_2 = NOW.plusHours(2);

    @InjectMocks
    private GetDeliveryPromiseProjectionUseCase getDeliveryPromiseUseCase;

    @Mock
    private CalculateCptProjectionUseCase calculatedProjectionUseCase;

    @Mock
    private ProcessingDistributionRepository processingDistRepository;

    @Mock
    private GetForecastUseCase getForecastUseCase;

    @Mock
    private GetCycleTimeUseCase getCycleTimeUseCase;

    @Mock
    private GetCptByWarehouseUseCase getCptByWarehouseUseCase;

    @ParameterizedTest
    @MethodSource("mockParameterizedConfiguration")
    public void testExecute(final String assertionsGroup,
                            final String warehouseId,
                            final List<DeliveryPromiseProjectionOutput> output,
                            final List<Backlog> backlogs,
                            final List<ZonedDateTime> cptDates) {
        //GIVEN
        final List<GetCptByWarehouseOutput> cptByWarehouse = mockCptByWarehouse();

        final GetDeliveryPromiseProjectionInput input = GetDeliveryPromiseProjectionInput.builder()
                .warehouseId(warehouseId)
                .workflow(Workflow.FBM_WMS_OUTBOUND)
                .dateFrom(NOW)
                .dateTo(NOW.plusHours(6))
                .backlog(backlogs)
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

        when(calculatedProjectionUseCase.execute(CptProjectionInput.builder()
                .workflow(input.getWorkflow())
                .logisticCenterId(input.getWarehouseId())
                .capacity(mockCapacityByHour())
                .backlog(input.getBacklog())
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .planningUnits(emptyList())
                .projectionType(DEFERRAL)
                .cptByWarehouse(cptByWarehouse)
                .currentDate(getCurrentUtcDate())
                .build())
        ).thenReturn(output.stream().map(item ->
                        new CptCalculationOutput(item.getDate(),
                                item.getProjectedEndDate(),
                                item.getRemainingQuantity()))
                .collect(Collectors.toList()));

        when(getCycleTimeUseCase.execute(new GetCycleTimeInput(warehouseId, cptDates)))
                .thenReturn(mockCycleTimeByCpt());

        when(getCptByWarehouseUseCase.execute(new
                GetCptByWarehouseInput(warehouseId, NOW, NOW.plusHours(6),
                cptDates,null))).thenReturn(cptByWarehouse);

        //WHEN
        final List<DeliveryPromiseProjectionOutput> response =
                getDeliveryPromiseUseCase.execute(input);

        //THEN
        if ("TestValues".equals(assertionsGroup)) {

            assertEquals(output.size(), response.size());
            assertEquals(output.get(0).getDate(), response.get(0).getDate());
            assertEquals(output.get(0).getProjectedEndDate(), response.get(0)
                    .getProjectedEndDate());
            assertEquals(output.get(0).getRemainingQuantity(), response.get(0)
                    .getRemainingQuantity());
            assertEquals(output.get(0).getEtdCutoff(), response.get(0).getEtdCutoff());
            assertEquals(output.get(0).isDeferred(), response.get(0).isDeferred());
            assertEquals(output.get(0).getProcessingTime(), response.get(0)
                    .getProcessingTime());
        }
        if ("TestSomeFieldNullPointerException".equals(assertionsGroup)) {

            assertEquals(output.isEmpty(), response.isEmpty());
        }
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

    private Map<ZonedDateTime, Configuration> mockCycleTimeByCpt() {

        final Map<ZonedDateTime, Configuration> ctByCpt = new HashMap<>();

        ctByCpt.put(CPT_1, Configuration.builder()
                .value(360L).metricUnit(MINUTES).key("processing_time").build());

        ctByCpt.put(CPT_2, Configuration.builder()
                .value(360L).metricUnit(MINUTES).key("processing_time").build());

        return ctByCpt;
    }

    private List<GetCptByWarehouseOutput> mockCptByWarehouse() {

        final List<GetCptByWarehouseOutput> getCptByWarehouseOutputs = new ArrayList<>();

        getCptByWarehouseOutputs.add(GetCptByWarehouseOutput.builder()
                .date(CPT_1)
                .processingTime(new ProcessingTime(360L, MINUTES)).build());

        getCptByWarehouseOutputs.add(GetCptByWarehouseOutput.builder()
                .date(CPT_2)
                .processingTime(new ProcessingTime(360L, MINUTES)).build());

        return getCptByWarehouseOutputs;
    }

    public static Stream<Arguments> mockParameterizedConfiguration() {
        return Stream.of(
                Arguments.of(
                        "TestValues",
                        WAREHOUSE_ID,
                        List.of(
                                new DeliveryPromiseProjectionOutput(
                                        CPT_1,
                                        CPT_1.minusHours(2),
                                        0,
                                        CPT_1.minusHours(6),
                                        new ProcessingTime(360L, MINUTES),
                                        CPT_1.minusHours(7),
                                        false),
                                new DeliveryPromiseProjectionOutput(
                                        CPT_2,
                                        CPT_2.minusHours(2),
                                        0,
                                        CPT_2.minusHours(6),
                                        new ProcessingTime(360L, MINUTES),
                                        CPT_1.minusHours(7),
                                        false)
                        ),
                        List.of(
                                new Backlog(CPT_1, 100),
                                new Backlog(CPT_2, 200)),
                        List.of(CPT_1, CPT_2)
                ),
                Arguments.of(
                        "TestSomeFieldNullPointerException",
                        WAREHOUSE_ID,
                        emptyList(),
                        emptyList(),
                        emptyList()
                )
        );
    }
}
