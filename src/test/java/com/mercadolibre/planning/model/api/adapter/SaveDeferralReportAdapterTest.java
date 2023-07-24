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
import com.mercadolibre.planning.model.api.domain.usecase.deferral.SaveDeferralReport.CptDeferred;
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
class SaveDeferralReportAdapterTest {

  private static final String ARTW01 = "ARTW01";

  private static final Instant DEFERRAL_DATE = Instant.parse("2023-07-19T15:00:00Z");

  private static final Instant CPT_DEFERRED_1_DATE = Instant.parse("2023-07-19T16:00:00Z");

  private static final Instant CPT_DEFERRED_2_DATE = Instant.parse("2023-07-19T17:00:00Z");

  private static final Instant CPT_DEFERRED_3_DATE = Instant.parse("2023-07-19T18:00:00Z");

  private static final CptDeferred CPT_DEFERRED_1 = new CptDeferred(CPT_DEFERRED_1_DATE, true, CAP_MAX);

  private static final CptDeferred CPT_DEFERRED_2 = new CptDeferred(CPT_DEFERRED_2_DATE, true, CASCADE);

  private static final CptDeferred CPT_DEFERRED_3 = new CptDeferred(CPT_DEFERRED_3_DATE, true, NOT_DEFERRED);

  private static final List<CptDeferred> CPT_DEFERRALS = List.of(CPT_DEFERRED_1, CPT_DEFERRED_2, CPT_DEFERRED_3);

  @Mock
  private OutboundDeferralDataRepository outboundDeferralDataRepository;

  @InjectMocks
  private SaveDeferralReportAdapter saveDeferralReportAdapter;

  @Captor
  private ArgumentCaptor<List<OutboundDeferralData>> outboundDeferralDataListCaptor;

  private static List<OutboundDeferralData> mockExpectedDeferralReport() {
    return CPT_DEFERRALS.stream()
        .map(cpt -> new OutboundDeferralData(
            ARTW01,
            DEFERRAL_DATE,
            cpt.getDate(),
            cpt.getStatus(),
            cpt.isUpdated()
        )).collect(Collectors.toList());
  }

  @Test
  @DisplayName("when searching units from multiple forecasts then overlapping date_ins belong to the latest forecast")
  void testSaveDeferralReport() {
    //GIVEN
    final var expectedDeferralReport = mockExpectedDeferralReport();
    when(outboundDeferralDataRepository.saveAll(any())).thenReturn(expectedDeferralReport);

    //WHEN
    saveDeferralReportAdapter.saveDeferralReport(
        ARTW01,
        DEFERRAL_DATE,
        CPT_DEFERRALS
    );

    //THEN
    verify(outboundDeferralDataRepository, times(1)).saveAll(outboundDeferralDataListCaptor.capture());
    assertEquals(expectedDeferralReport, outboundDeferralDataListCaptor.getValue());
  }

  @Test
  @DisplayName("Gets and exception when repository saveAll")
  void testSaveDeferralReportException() {
    //GIVEN
    when(outboundDeferralDataRepository.saveAll(any())).thenThrow(IllegalArgumentException.class);

    //WHEN
    assertThrows(IllegalArgumentException.class, () -> saveDeferralReportAdapter.saveDeferralReport(
        ARTW01,
        DEFERRAL_DATE,
        CPT_DEFERRALS
    ));
  }
}

