package com.mercadolibre.planning.model.api.config;

import com.mercadolibre.configurationservice.sdk.client.ProfileManager;
import com.mercadolibre.configurationservice.sdk.client.ProfileManagerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class ConfigServiceFileManager {

  @Bean
  @Profile("development")
  public ProfileManager configurationManagerLocal() {
    return ProfileManagerBuilder
        .builder()
        .withBasePath("configs")
        .withChecksumDisabled()
        .build();
  }

  @Bean
  @Profile("!development")
  public ProfileManager configurationManager() {
    return ProfileManagerBuilder
        .builder()
        .build();
  }

}
