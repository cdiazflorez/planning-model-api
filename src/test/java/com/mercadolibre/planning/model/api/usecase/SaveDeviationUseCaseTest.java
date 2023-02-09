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
import com.mercadolibre.planning.model.api.domain.entity.Path;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.save.SaveDeviationInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.save.SaveDeviationUseCase;
import com.mercadolibre.planning.model.api.web.controller.deviation.response.DeviationResponse;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
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

    final var saved2 = List.of(saved);

    // WHEN
    when(deviationRepository.saveAll(any(List.class))).thenReturn(saved2);

    final DeviationResponse output = useCase.execute(List.of(input));

    // THEN

    verify(deviationRepository).saveAll(any(List.class));
    assertEquals(200L, output.getStatus());
  }

  @Test
  public void testSaveInbounSellerdDeviationOk() {
    // GIVEN
    final SaveDeviationInput input = mockSaveInboundDeviation(INBOUND);

    final CurrentForecastDeviation saved = mockCurrentForecastDeviation(INBOUND);

    // WHEN
    when(deviationRepository.saveAll(any(List.class))).thenReturn(mockCurrentForecastDeviationSaved(INBOUND));

    final DeviationResponse output = useCase.execute(List.of(input));

    // THEN
    verify(deviationRepository).saveAll(any(List.class));
    assertEquals(200L, output.getStatus());
  }

  @Test
  public void testSaveInboundTransferDeviationOk() {
    // GIVEN
    final SaveDeviationInput input = mockSaveInboundDeviation(INBOUND_TRANSFER);

    final CurrentForecastDeviation saved = mockCurrentForecastDeviation(INBOUND_TRANSFER);

    // WHEN
    when(deviationRepository.saveAll(any(List.class))).thenReturn(mockCurrentForecastDeviationSaved(INBOUND_TRANSFER));

    final DeviationResponse output = useCase.execute(List.of(input));

    // THEN
    verify(deviationRepository).saveAll(any(List.class));
    assertEquals(200L, output.getStatus());
  }

  private SaveDeviationInput mockSaveInboundDeviation(final Workflow workflow) {
    return SaveDeviationInput
        .builder()
        .workflow(workflow)
        .dateFrom(Instant.now().atZone(ZoneId.of("America/Argentina/Buenos_Aires")))
        .dateTo(Instant.now().plus(1, ChronoUnit.DAYS).atZone(ZoneId.of("America/Argentina/Buenos_Aires")))
        .value(10.0)
        .userId(1234L)
        .paths(List.of(Path.PRIVATE, Path.FTL))
        .warehouseId("ARTW01")
        .deviationType(UNITS)
        .build();
  }

  private List<CurrentForecastDeviation> mockCurrentForecastDeviationSaved(final Workflow workflow) {
    return List.of(
        CurrentForecastDeviation.builder()
          .logisticCenterId("ARTW01")
          .dateFrom(Instant.now().atZone(ZoneId.of("America/Argentina/Buenos_Aires")))
          .dateTo(Instant.now().plus(1, ChronoUnit.DAYS).atZone(ZoneId.of("America/Argentina/Buenos_Aires")))
          .value(10)
          .isActive(true)
          .userId(1234L)
          .workflow(workflow)
          .type(UNITS)
          .path(Path.PRIVATE)
          .build(),
        CurrentForecastDeviation.builder()
            .logisticCenterId("ARTW01")
            .dateFrom(Instant.now().atZone(ZoneId.of("America/Argentina/Buenos_Aires")))
            .dateTo(Instant.now().plus(1, ChronoUnit.DAYS).atZone(ZoneId.of("America/Argentina/Buenos_Aires")))
            .value(10)
            .isActive(true)
            .userId(1234L)
            .workflow(workflow)
            .type(UNITS)
            .path(Path.FTL)
            .build()
    );
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
