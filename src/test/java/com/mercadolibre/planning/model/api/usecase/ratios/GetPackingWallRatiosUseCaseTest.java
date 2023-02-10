package com.mercadolibre.planning.model.api.usecase.ratios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.mercadolibre.planning.model.api.domain.usecase.ratios.GetPackingWallRatiosUseCase;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetPackingWallRatiosUseCaseTest {

  private static final Instant DATE_FROM = Instant.parse("2023-01-25T18:30:00Z");

  private static final Instant DATE_TO = Instant.parse("2023-02-03T21:30:00Z");

  private static final String ARTW01 = "ARTW01";

  @InjectMocks
  private GetPackingWallRatiosUseCase getPackingWallRatiosUseCase;

  @Test
  void sampleTest() {
    //ToDo improve GetPackingWallRatiosUseCaseTest when UseCase will be finished
    final var result = getPackingWallRatiosUseCase.execute(ARTW01, DATE_FROM, DATE_TO);
    assertNotNull(result);
    assertEquals(Double.NaN, result.get(DATE_FROM).getPackingWallRatio());
    assertEquals(Double.NaN, result.get(DATE_FROM).getPackingToteRatio());
  }
}
