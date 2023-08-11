package com.mercadolibre.planning.model.api.domain.usecase.deferral;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GetDeferralRepostTest {

  private static final String LOGISTIC_CENTER_ID = "ARTW01";

  private static final Instant DATE_FROM = Instant.parse("2023-08-08T00:00:00Z");

  private static final Instant DATE_TO = Instant.parse("2023-08-09T00:00:00Z");

  private static final Instant DATE_1 = Instant.parse("2023-08-08T10:00:00Z");

  private static final Instant DATE_2 = Instant.parse("2023-08-08T10:30:00Z");

  private static final Instant DATE_3 = Instant.parse("2023-08-08T20:00:00Z");

  private static final Instant SLA_1 = Instant.parse("2023-08-09T00:00:00Z");

  private static final Instant SLA_2 = Instant.parse("2023-08-09T01:00:00Z");

  private static final Map<Instant, List<GetDeferralReport.SlaStatus>> DEFERRAL_REPORT = Map.of(
      DATE_1, List.of(
          new GetDeferralReport.SlaStatus(SLA_1, DeferralType.NOT_DEFERRED),
          new GetDeferralReport.SlaStatus(SLA_2, DeferralType.CAP_MAX)
      ),
      DATE_2, List.of(
          new GetDeferralReport.SlaStatus(SLA_1, DeferralType.NOT_DEFERRED),
          new GetDeferralReport.SlaStatus(SLA_2, DeferralType.NOT_DEFERRED)
      ),
      DATE_3, List.of(
          new GetDeferralReport.SlaStatus(SLA_1, DeferralType.CASCADE),
          new GetDeferralReport.SlaStatus(SLA_2, DeferralType.CAP_MAX)
      )
  );

  private static final List<GetDeferralReport.Deferral> DEFERRALS = List.of(
      new GetDeferralReport.Deferral(DATE_1, SLA_1, DeferralType.NOT_DEFERRED),
      new GetDeferralReport.Deferral(DATE_1, SLA_2, DeferralType.CAP_MAX),
      new GetDeferralReport.Deferral(DATE_2, SLA_1, DeferralType.NOT_DEFERRED),
      new GetDeferralReport.Deferral(DATE_2, SLA_2, DeferralType.NOT_DEFERRED),
      new GetDeferralReport.Deferral(DATE_3, SLA_1, DeferralType.CASCADE),
      new GetDeferralReport.Deferral(DATE_3, SLA_2, DeferralType.CAP_MAX)
  );

  @InjectMocks
  private GetDeferralReport getDeferralReport;

  @Mock
  private GetDeferralReport.DeferralHistoryGateway deferralHistoryGateway;

  @Test
  void testGetDeferralReport() {
    when(deferralHistoryGateway.getDeferralReport(LOGISTIC_CENTER_ID, DATE_FROM, DATE_TO))
        .thenReturn(DEFERRALS);

    final var response = getDeferralReport.execute(LOGISTIC_CENTER_ID, DATE_FROM, DATE_TO);

    assertEquals(DEFERRAL_REPORT.get(DATE_1), response.get(DATE_1));
    assertEquals(DEFERRAL_REPORT.get(DATE_2), response.get(DATE_2));
    assertEquals(DEFERRAL_REPORT.get(DATE_3), response.get(DATE_3));
  }

  @Test
  void notGetDeferralReportTest() {

    when(deferralHistoryGateway.getDeferralReport(LOGISTIC_CENTER_ID, DATE_FROM, DATE_TO))
        .thenReturn(emptyList());

    final var response = getDeferralReport.execute(LOGISTIC_CENTER_ID, DATE_FROM, DATE_TO);

    assertEquals(emptyMap(), response);
  }

}
