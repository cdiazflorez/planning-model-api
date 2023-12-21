package com.mercadolibre.planning.model.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.configurationservice.sdk.client.ProfileManager;
import com.mercadolibre.planning.model.api.domain.service.configuration.ProcessingTimeService.FuryConfigServiceGateway;
import com.mercadolibre.planning.model.api.exception.ReadFuryConfigException;
import java.io.IOException;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class FuryConfigService implements FuryConfigServiceGateway {

  private static final String DEFAULT_KEY = "default";

  private final ProfileManager profileManager;

  private final ObjectMapper objectMapper;

  private DefaultValues defaultValues;

  public FuryConfigService(final ProfileManager profileManager, final ObjectMapper objectMapper) {
    this.profileManager = profileManager;
    this.objectMapper = objectMapper;
  }

  public void loadDefaultValues() {
    try {
      defaultValues = parse(profileManager.read("default-values"));
    } catch (IOException e) {
      throw new ReadFuryConfigException(e.getMessage(), e);
    }
  }

  private DefaultValues parse(final byte[] profileContent) throws IOException {
    return objectMapper.readValue(profileContent, DefaultValues.class);
  }

  public int getProcessingTime(final String logisticCenterId) {
    if (defaultValues == null) {
      loadDefaultValues();
    }
    final Map<String, Integer> processingTimes = defaultValues.processingTimes;
    return processingTimes.getOrDefault(logisticCenterId, processingTimes.get(DEFAULT_KEY));
  }

  private record DefaultValues(Map<String, Integer> processingTimes) {
  }

}
