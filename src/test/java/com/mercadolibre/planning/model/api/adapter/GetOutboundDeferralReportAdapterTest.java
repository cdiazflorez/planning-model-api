package com.mercadolibre.planning.model.api.adapter;

import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.client.db.repository.deferral.OutboundDeferralDataRepository;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.deferral.OutboundDeferralData;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.DeferralType;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.GetDeferralReport;
import java.time.Instant;
import java.util.List;
import java.util.Set;
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
public class GetOutboundDeferralReportAdapterTest {

  private static final String WAREHOUSE_ID = "ARTW01";

  private static final Set<DeferralType> DEFERRAL_TYPES = Set.of(DeferralType.CAP_MAX, DeferralType.CASCADE);

  private static final Instant DATE_IN_1 = A_DATE_UTC.toInstant();
  private static final Instant DATE_IN_2 = A_DATE_UTC.toInstant();

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
            DATE,
            CPT,
            DeferralType.CAP_MAX
        ),
        new GetDeferralReport.Deferral(
            DATE,
            CPT,
            DeferralType.CASCADE
        ));
  }

  private static Stream<Arguments> provideArgumentsAndExpectedValuesFromMultipleDeferredCpts() {
    return Stream.of(
        Arguments.of(
            mockResponseForDeferredCpts(),
            List.of(DATE_IN_1, DATE_IN_2)
        ),
        Arguments.of(
            List.of(),
            List.of()
        )
    );
  }

  private static List<Instant> mockResponseForDeferredCpts() {
    return List.of(
        Instant.parse("2020-08-19T17:00:00Z"),
        Instant.parse("2020-08-19T17:00:00Z")
    );
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

  @ParameterizedTest
  @MethodSource("provideArgumentsAndExpectedValuesFromMultipleDeferredCpts")
  @DisplayName("Should return a list of deferred CPTs")
  void testGetDeferredCptsOk(final List<Instant> deferralDto,
                             final List<Instant> expected) {
    // GIVEN

    when(deferralRepository.findDeferredCpts(WAREHOUSE_ID, DATE_IN_1, DEFERRAL_TYPES))
        .thenReturn(deferralDto);

    // WHEN
    final var result = getDeferralReportAdapter.getDeferredCpts(
        WAREHOUSE_ID,
        Workflow.FBM_WMS_OUTBOUND,
        A_DATE_UTC.toInstant());

    // THEN
    assertNotNull(result);
    assertEquals(expected, result);
  }

}
