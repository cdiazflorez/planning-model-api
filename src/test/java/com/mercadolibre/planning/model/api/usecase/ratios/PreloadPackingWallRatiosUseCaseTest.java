package com.mercadolibre.planning.model.api.usecase.ratios;

import com.mercadolibre.planning.model.api.domain.usecase.ratios.PreloadPackingWallRatiosUseCase;
import java.time.Instant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PreloadPackingWallRatiosUseCaseTest {

  @InjectMocks
  private PreloadPackingWallRatiosUseCase preloadPackingWallRatiosUseCase;

  @Test
  void sampleTest() {
    //ToDo improve PreloadPackingWallRatiosUseCaseTests when UseCase will be finished
    final var result = preloadPackingWallRatiosUseCase.execute(Instant.now(), Instant.now());
    Assertions.assertNotNull(result);
    Assertions.assertEquals(0, result.size());
  }
}
