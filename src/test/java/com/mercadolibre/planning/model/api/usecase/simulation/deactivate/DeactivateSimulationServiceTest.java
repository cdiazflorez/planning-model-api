package com.mercadolibre.planning.model.api.usecase.simulation.deactivate;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.CHECK_IN;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.USER_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentHeadcountProductivityRepository;
import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.deactivate.DeactivateSimulationInput;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.deactivate.DeactivateSimulationService;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DeactivateSimulationServiceTest {

    @InjectMocks
    private DeactivateSimulationService deactivateSimulationService;

    @Mock
    private CurrentProcessingDistributionRepository currentProcessingDistributionRepository;

    @Mock
    private CurrentHeadcountProductivityRepository currentHeadcountProductivityRepository;

    @ParameterizedTest
    @MethodSource("provideListOfDeactivateSimulationInputArguments")
    public void deactivateSimulationOkTest(final List<DeactivateSimulationInput> deactivateSimulationInputs) {
        //GIVEN
        //WHEN
        deactivateSimulationService.deactivateSimulation(deactivateSimulationInputs);
        //THEN
        verify(currentProcessingDistributionRepository, times(deactivateSimulationInputs.size())).deactivateProcessingDistribution(
                anyString(),
                any(Workflow.class),
                any(ProcessName.class),
                anyList(),
                any(ProcessingType.class),
                anyLong(),
                any(MetricUnit.class)
        );
        verify(currentHeadcountProductivityRepository, times(deactivateSimulationInputs.size())).deactivateProductivity(
                anyString(),
                any(Workflow.class),
                any(ProcessName.class),
                anyList(),
                any(MetricUnit.class),
                anyLong(),
                anyInt()
        );
    }

    private static Stream<Arguments> provideListOfDeactivateSimulationInputArguments() {

        List<ZonedDateTime> dates = List.of(
                A_DATE_UTC,
                A_DATE_UTC.plus(1, HOURS),
                A_DATE_UTC.plus(2, HOURS)
        );

        return Stream.of(
                Arguments.of(
                        List.of(
                             new DeactivateSimulationInput(
                                     WAREHOUSE_ID,
                                     FBM_WMS_INBOUND,
                                     CHECK_IN,
                                     dates,
                                     USER_ID
                             ),
                                new DeactivateSimulationInput(
                                        WAREHOUSE_ID,
                                        FBM_WMS_INBOUND,
                                        PUT_AWAY,
                                        dates,
                                        USER_ID
                                )
                        )
                ),
                Arguments.of(
                        List.of(
                                new DeactivateSimulationInput(
                                        WAREHOUSE_ID,
                                        FBM_WMS_OUTBOUND,
                                        PICKING,
                                        dates,
                                        USER_ID
                                ),
                                new DeactivateSimulationInput(
                                        WAREHOUSE_ID,
                                        FBM_WMS_OUTBOUND,
                                        PACKING,
                                        dates,
                                        USER_ID
                                ),
                                new DeactivateSimulationInput(
                                        WAREHOUSE_ID,
                                        FBM_WMS_OUTBOUND,
                                        PACKING_WALL,
                                        dates,
                                        USER_ID
                                )
                        )
                )
        );
    }

}
