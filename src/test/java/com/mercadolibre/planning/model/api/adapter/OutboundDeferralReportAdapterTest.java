package com.mercadolibre.planning.model.api.adapter;

import static com.mercadolibre.planning.model.api.domain.usecase.deferral.DeferralType.CAP_MAX;
import static com.mercadolibre.planning.model.api.domain.usecase.deferral.DeferralType.CASCADE;
import static com.mercadolibre.planning.model.api.domain.usecase.deferral.DeferralType.NOT_DEFERRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.client.db.repository.deferral.OutboundDeferralDataRepository;
import com.mercadolibre.planning.model.api.domain.entity.deferral.OutboundDeferralData;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.SaveOutboundDeferralReport.CptDeferralReport;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboundDeferralReportAdapterTest {

  private static final String ARTW01 = "ARTW01";

  private static final Instant DEFERRAL_DATE = Instant.parse("2023-07-19T15:00:00Z");

  private static final int DELETED_REPORTS_QTY = 5;

  private static final Instant CPT_DEFERRED_1_DATE = Instant.parse("2023-07-19T16:00:00Z");

  private static final Instant CPT_DEFERRED_2_DATE = Instant.parse("2023-07-19T17:00:00Z");

  private static final Instant CPT_DEFERRED_3_DATE = Instant.parse("2023-07-19T18:00:00Z");

  private static final CptDeferralReport CPT_DEFERRED_1 = new CptDeferralReport(CPT_DEFERRED_1_DATE, true, CAP_MAX);

  private static final CptDeferralReport CPT_DEFERRED_2 = new CptDeferralReport(CPT_DEFERRED_2_DATE, true, CASCADE);

  private static final CptDeferralReport CPT_DEFERRED_3 = new CptDeferralReport(CPT_DEFERRED_3_DATE, true, NOT_DEFERRED);

  private static final List<CptDeferralReport> CPT_DEFERRALS = List.of(CPT_DEFERRED_1, CPT_DEFERRED_2, CPT_DEFERRED_3);

  @Mock
  private OutboundDeferralDataRepository outboundDeferralDataRepository;

  @InjectMocks
  private OutboundDeferralReportAdapter outboundDeferralReportAdapter;

  @Captor
  private ArgumentCaptor<List<OutboundDeferralData>> outboundDeferralDataListCaptor;

  private static List<OutboundDeferralData> mockExpectedDeferralReport() {
    return CPT_DEFERRALS.stream()
        .map(cpt -> new OutboundDeferralData(
            ARTW01,
            DEFERRAL_DATE,
            cpt.date(),
            cpt.status(),
            cpt.updated()
        )).collect(Collectors.toList());
  }

  @Test
  @DisplayName("Saves all deferral reports on repository saveAll")
  void testSaveDeferralReport() {
    //GIVEN
    final var expectedDeferralReport = mockExpectedDeferralReport();
    when(outboundDeferralDataRepository.saveAll(any())).thenReturn(expectedDeferralReport);

    //WHEN
    outboundDeferralReportAdapter.saveDeferralReport(
        ARTW01,
        DEFERRAL_DATE,
        CPT_DEFERRALS
    );

    //THEN
    verify(outboundDeferralDataRepository, times(1)).saveAll(outboundDeferralDataListCaptor.capture());
    assertEquals(expectedDeferralReport, outboundDeferralDataListCaptor.getValue());
  }

  @Test
  @DisplayName("Throws an exception when repository saveAll")
  void testSaveDeferralReportException() {
    //GIVEN
    when(outboundDeferralDataRepository.saveAll(any())).thenThrow(IllegalArgumentException.class);

    //WHEN
    assertThrows(IllegalArgumentException.class, () -> outboundDeferralReportAdapter.saveDeferralReport(
        ARTW01,
        DEFERRAL_DATE,
        CPT_DEFERRALS
    ));
  }

  @Test
  @DisplayName("Removes the repository entries where the date is before given date")
  void testDeleteDeferralReportBeforeDate() {
    //GIVEN
    when(outboundDeferralDataRepository.deleteByDateBefore(DEFERRAL_DATE)).thenReturn(DELETED_REPORTS_QTY);

    //WHEN
    final int deletedReportsQty = outboundDeferralReportAdapter.deleteDeferralReportBeforeDate(DEFERRAL_DATE);

    //THEN
    verify(outboundDeferralDataRepository, times(1)).deleteByDateBefore(DEFERRAL_DATE);
    assertEquals(DELETED_REPORTS_QTY, deletedReportsQty);
  }
}

