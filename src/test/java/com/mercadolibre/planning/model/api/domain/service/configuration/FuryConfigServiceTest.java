package com.mercadolibre.planning.model.api.domain.service.configuration;

import static com.mercadolibre.planning.model.api.util.TestUtils.objectMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.mercadolibre.configurationservice.sdk.client.ProfileManager;
import com.mercadolibre.planning.model.api.config.FuryConfigService;
import com.mercadolibre.planning.model.api.exception.ReadFuryConfigException;
import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FuryConfigServiceTest {

  private static final String DEFAULT_VALUES_FILENAME = "default-values.json";

  private static final String ARBA_01 = "ARBA01";

  private static final String ARTW_01 = "ARTW01";

  private static final int PT_240 = 240;

  private static final int PT_165 = 165;

  private static final String DEFAULT_VALUES_JSON = """
      {
        "processing_times": {
          "default": 240,
          "ARTW01": 165
        }
      }
      """;

  @Mock
  private ProfileManager profileManager;

  private FuryConfigService furyConfigService;

  private static Stream<Arguments> provideArgumentsGetProcessingTime() {
    return Stream.of(
        Arguments.of(ARBA_01, PT_240),
        Arguments.of(ARTW_01, PT_165)
    );
  }

  @ParameterizedTest
  @MethodSource("provideArgumentsGetProcessingTime")
  void testConfigServiceOk(final String logisticCenterId, final int expectedProcessingTime) throws IOException {
    // GIVEN
    when(profileManager.read(DEFAULT_VALUES_FILENAME))
        .thenReturn(DEFAULT_VALUES_JSON.getBytes());
    // WHEN
    furyConfigService = new FuryConfigService(profileManager, objectMapper());

    final int processingTime = furyConfigService.getProcessingTime(logisticCenterId);

    // THEN
    assertEquals(expectedProcessingTime, processingTime);
  }

  @Test
  void testConfigServiceException() throws IOException {
    // GIVEN
    doThrow(IOException.class).when(profileManager).read(DEFAULT_VALUES_FILENAME);
    furyConfigService = new FuryConfigService(profileManager, objectMapper());

    // WHEN AND THEN
    assertThrows(
        ReadFuryConfigException.class,
        furyConfigService::loadDefaultValues
    );
  }

}
