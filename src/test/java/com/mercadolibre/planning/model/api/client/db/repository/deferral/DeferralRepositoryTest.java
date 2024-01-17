package com.mercadolibre.planning.model.api.client.db.repository.deferral;

import static com.mercadolibre.planning.model.api.domain.usecase.deferral.DeferralType.CAP_MAX;
import static com.mercadolibre.planning.model.api.domain.usecase.deferral.DeferralType.CASCADE;
import static com.mercadolibre.planning.model.api.domain.usecase.deferral.DeferralType.NOT_DEFERRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mercadolibre.planning.model.api.domain.entity.deferral.OutboundDeferralData;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.DeferralType;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DeferralRepositoryTest {

  private static final String WAREHOUSE_ID = "ARTW01";

  private static final Set<DeferralType> DEFERRAL_TYPES = Set.of(CAP_MAX, CASCADE);

  private static final Instant VIEW_DATE = Instant.parse("2022-09-10T10:00:00Z");

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
            5
        ),
        Arguments.of(
            Instant.parse("2022-09-08T09:00:00Z"),
            Instant.parse("2022-09-08T14:00:00Z"),
            5
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
      final var ids = idViews.stream().map(OutboundDeferralData::getId).toList();
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
    assertEquals(8, removedEntries);
  }

  @Test
  @Sql("/sql/forecast/load_deferral.sql")
  void testFindByLogisticCenterIdAndDateBeforeAndCpt() {
    // WHEN
    final List<OutboundDeferralData> deferrals = repository.findDeferredCpts(
        WAREHOUSE_ID,
        VIEW_DATE,
        DEFERRAL_TYPES
    );

    // THEN
    assertEquals(2, deferrals.size());
    deferrals.forEach(deferral -> {
      assertTrue(deferral.getCpt().isBefore(VIEW_DATE));
    });
  }
}
