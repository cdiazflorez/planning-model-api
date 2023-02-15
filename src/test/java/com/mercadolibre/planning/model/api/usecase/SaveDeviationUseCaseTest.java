package com.mercadolibre.planning.model.api.usecase;

import static com.mercadolibre.planning.model.api.domain.entity.DeviationType.UNITS;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.INBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.INBOUND_TRANSFER;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockSaveForecastDeviationInput;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.CurrentForecastDeviationRepository;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.save.SaveDeviationInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.save.SaveDeviationUseCase;
import com.mercadolibre.planning.model.api.web.controller.deviation.response.DeviationResponse;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SaveDeviationUseCaseTest {

  @InjectMocks
  private SaveDeviationUseCase useCase;

  @Mock
  private CurrentForecastDeviationRepository deviationRepository;

  @Test
  public void testSaveForecastDeviationOk() {
    // GIVEN

    final SaveDeviationInput input = mockSaveForecastDeviationInput();

    final CurrentForecastDeviation saved = CurrentForecastDeviation
        .builder()
        .workflow(FBM_WMS_OUTBOUND)
        .id(1L)
        .build();

    // WHEN
    when(deviationRepository.save(any(CurrentForecastDeviation.class))).thenReturn(saved);

    final DeviationResponse output = useCase.execute(input);

    // THEN

    verify(deviationRepository).save(any(CurrentForecastDeviation.class));
    assertEquals(200L, output.getStatus());
  }

  @Test
  public void testSaveInbounSellerdDeviationOk() {
    // GIVEN
    final SaveDeviationInput input = mockSaveInboundDeviation(INBOUND);

    final CurrentForecastDeviation saved = mockCurrentForecastDeviation(INBOUND);

    // WHEN
    when(deviationRepository.save(any(CurrentForecastDeviation.class))).thenReturn(saved);

    final DeviationResponse output = useCase.execute(input);

    // THEN
    verify(deviationRepository).save(any(CurrentForecastDeviation.class));
    assertEquals(200L, output.getStatus());
  }

  @Test
  public void testSaveInboundTramsferDeviationOk() {
    // GIVEN
    final SaveDeviationInput input = mockSaveInboundDeviation(INBOUND_TRANSFER);

    final CurrentForecastDeviation saved = mockCurrentForecastDeviation(INBOUND_TRANSFER);

    // WHEN
    when(deviationRepository.save(any(CurrentForecastDeviation.class))).thenReturn(saved);

    final DeviationResponse output = useCase.execute(input);

    // THEN
    verify(deviationRepository).save(any(CurrentForecastDeviation.class));
    assertEquals(200L, output.getStatus());
  }

  private SaveDeviationInput mockSaveInboundDeviation(final Workflow workflow) {
    return SaveDeviationInput
        .builder()
        .workflow(workflow)
        .dateFrom(Instant.now().atZone(ZoneId.of("America/Argentina/Buenos_Aires")))
        .dateTo(Instant.now().plus(1, ChronoUnit.DAYS).atZone(ZoneId.of("America/Argentina/Buenos_Aires")))
        .value(0.1)
        .userId(1234L)
        .warehouseId("ARTW01")
        .deviationType(UNITS)
        .build();
  }

  private CurrentForecastDeviation mockCurrentForecastDeviation(final Workflow workflow) {
    return CurrentForecastDeviation
        .builder()
        .workflow(workflow)
        .id(1L)
        .type(UNITS)
        .build();
  }
}
