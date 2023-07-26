package com.mercadolibre.planning.model.api.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.client.db.repository.deferral.OutboundDeferralDataRepository;
import com.mercadolibre.planning.model.api.domain.entity.deferral.OutboundDeferralData;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.DeferralType;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.GetDeferralReport;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GetDeferralReportAdapterTest {

    private static final String WAREHOUSE_ID = "ARTW01";

    private static final Instant DATE = Instant.parse("2022-09-08T10:00:00Z");

    private static final Instant CPT = Instant.parse("2022-09-08T12:00:00Z");

    @Mock
    OutboundDeferralDataRepository deferralRepository;

    @InjectMocks
    private GetDeferralReportAdapter getDeferralReportAdapter;

    private static Stream<Arguments> provideArgumentsAndExpectedValuesFromMultipleDeferral() {
        return Stream.of(
                Arguments.of(
                        mockResponse(),
                        mockExpected()
                ),
                Arguments.of(
                        List.of(),
                        List.of()
                )
        );
    }

    private static List<OutboundDeferralData> mockResponse() {
        return List.of(new OutboundDeferralData(
                        1L,
                        WAREHOUSE_ID,
                        DATE,
                        CPT,
                        DeferralType.CAP_MAX,
                        true
                ),
                new OutboundDeferralData(
                        2L,
                        WAREHOUSE_ID,
                        DATE,
                        CPT,
                        DeferralType.CASCADE,
                        true
                ));
    }

    private static List<GetDeferralReport.Deferral> mockExpected() {
        return List.of(new GetDeferralReport.Deferral(
                        CPT,
                        DeferralType.CAP_MAX,
                        true
                ),
                new GetDeferralReport.Deferral(
                        CPT,
                        DeferralType.CASCADE,
                        true
                ));
    }

    @ParameterizedTest
    @MethodSource("provideArgumentsAndExpectedValuesFromMultipleDeferral")
    @DisplayName("Should return a list of deferrals")
    void testGetDeferralReportOk(final List<OutboundDeferralData> deferralDto,
                                 final List<GetDeferralReport.Deferral> expected) {
        // GIVEN
        when(deferralRepository.findByLogisticCenterIdAndDateBetweenAndUpdatedIsTrue(WAREHOUSE_ID, DATE, CPT))
                .thenReturn(deferralDto);

        // WHEN
        final var result = getDeferralReportAdapter.getDeferralReport(WAREHOUSE_ID, DATE, CPT);

        // THEN
        assertNotNull(result);
        assertEquals(expected, result);
    }

}
