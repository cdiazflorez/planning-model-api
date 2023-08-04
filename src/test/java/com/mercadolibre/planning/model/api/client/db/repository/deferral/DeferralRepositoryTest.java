package com.mercadolibre.planning.model.api.client.db.repository.deferral;

import static com.mercadolibre.planning.model.api.domain.usecase.deferral.DeferralType.CAP_MAX;
import static com.mercadolibre.planning.model.api.domain.usecase.deferral.DeferralType.CASCADE;
import static com.mercadolibre.planning.model.api.domain.usecase.deferral.DeferralType.NOT_DEFERRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mercadolibre.planning.model.api.domain.entity.deferral.OutboundDeferralData;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest
class DeferralRepositoryTest {

  private static final String WAREHOUSE_ID = "ARTW01";

  private static final Instant DATE_FROM = Instant.parse("2022-09-08T10:00:00Z");

  private static final Instant DATE_TO = Instant.parse("2022-09-08T11:00:00Z");

  private static final Instant DELETE_BEFORE_DATE = Instant.parse("2022-09-11T11:00:00Z");

  private static final Instant DEFERRAL_DATE = Instant.parse("2022-09-28T11:00:00Z");

  @Autowired
  private OutboundDeferralDataRepository repository;

  private static Stream<Arguments> provideDateRanges() {
    return Stream.of(
        Arguments.of(
            DATE_FROM,
            DATE_TO,
            2
        ),
        Arguments.of(
            Instant.parse("2022-09-08T09:00:00Z"),
            Instant.parse("2022-09-08T14:00:00Z"),
            2
        )
    );
  }

  private static List<OutboundDeferralData> mockLastOutboundDeferralData() {
    return List.of(
        new OutboundDeferralData(9, WAREHOUSE_ID, DEFERRAL_DATE, Instant.parse("2022-09-28T18:00:00Z"), CAP_MAX, true),
        new OutboundDeferralData(10, WAREHOUSE_ID, DEFERRAL_DATE, Instant.parse("2022-09-28T19:00:00Z"), CASCADE, true),
        new OutboundDeferralData(11, WAREHOUSE_ID, DEFERRAL_DATE, Instant.parse("2022-09-28T20:00:00Z"), CASCADE, true),
        new OutboundDeferralData(12, WAREHOUSE_ID, DEFERRAL_DATE, Instant.parse("2022-09-28T21:00:00Z"), NOT_DEFERRED, false)
    );
  }

  @ParameterizedTest
  @MethodSource("provideDateRanges")
  @Sql("/sql/forecast/load_deferral.sql")
  void testSearchDeferralWithLogisticCenterShouldReturnDeferralsBetweenDate(final Instant dateFrom,
                                                                            final Instant dateTo,
                                                                            final int expectedDeferrals) {
    // GIVEN

    // WHEN
    final var idViews = repository.findByLogisticCenterIdAndDateBetweenAndUpdatedIsTrue(
        WAREHOUSE_ID,
        dateFrom,
        dateTo
    );

    // THEN
    assertEquals(expectedDeferrals, idViews.size());
    if (expectedDeferrals > 0) {
      final var ids = idViews.stream().map(OutboundDeferralData::getId).collect(Collectors.toList());
      assertTrue(ids.contains(1L));
      assertTrue(ids.contains(2L));
    }
  }

  @Test
  @Sql("/sql/forecast/load_deferral.sql")
  void testDeleteByDateBefore() {
    // WHEN
    final var removedEntries = repository.deleteByDateBefore(DELETE_BEFORE_DATE);

    // THEN
    assertEquals(6, removedEntries);
  }

  @Test
  @Sql("/sql/forecast/load_deferral.sql")
  void testGetLastCptDeferralReportForLogisticCenter() {
    // GIVEN
    final var expectedLastCptReport = mockLastOutboundDeferralData();
    // WHEN
    final var lastCptDeferralReportForLogisticCenter = repository.getLastCptDeferralReportForLogisticCenter(WAREHOUSE_ID);

    // THEN
    assertEquals(expectedLastCptReport, lastCptDeferralReportForLogisticCenter);
  }
}
