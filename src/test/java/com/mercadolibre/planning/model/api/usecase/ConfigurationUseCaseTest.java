package com.mercadolibre.planning.model.api.usecase;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.NA;
import static com.mercadolibre.planning.model.api.util.TestUtils.LOGISTIC_CENTER_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mercadolibre.planning.model.api.client.db.repository.configuration.ConfigurationRepository;
import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.entity.configuration.ConfigurationId;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.ConfigurationUseCase;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class ConfigurationUseCaseTest {

  private static final String ORDERS_PER_PALLET_RATIO = "orders_per_pallet_ratio";

  private static final String ORDERS_PER_PALLET_RATIO_VALUE = "15.5";

  private static final String UNITS_PER_ORDER_RATIO = "units_per_order_ratio";

  private static final String UNITS_PER_ORDER_RATIO_VALUE = "4.67";

  private static final String UNITS_PER_TOTE_RATIO = "units_per_tote_ratio";

  private static final String UNITS_PER_TOTE_RATIO_OLD_VALUE = "1.11";

  private static final String UNITS_PER_TOTE_RATIO_NEW_VALUE = "2.33";

  @Autowired
  private ConfigurationRepository configurationRepository;

  private ConfigurationUseCase configurationUseCase;

  @BeforeEach
  void setUp() {
    configurationUseCase = new ConfigurationUseCase(configurationRepository);
  }

  @Test
  void testCreateOrUpdateConfiguration() {
    //GIVEN
    configurationRepository.save(Configuration.builder()
                                     .logisticCenterId(LOGISTIC_CENTER_ID)
                                     .key(UNITS_PER_TOTE_RATIO)
                                     .value(UNITS_PER_TOTE_RATIO_OLD_VALUE)
                                     .metricUnit(NA)
                                     .lastUserUpdated(1L)
                                     .build());

    final ConcurrentHashMap<String, String> configsToSave = new ConcurrentHashMap<>();
    configsToSave.put(ORDERS_PER_PALLET_RATIO, ORDERS_PER_PALLET_RATIO_VALUE);
    configsToSave.put(UNITS_PER_ORDER_RATIO, UNITS_PER_ORDER_RATIO_VALUE);
    configsToSave.put(UNITS_PER_TOTE_RATIO, UNITS_PER_TOTE_RATIO_NEW_VALUE);

    final ConcurrentHashMap<ConfigurationId, String> expectedResponse = new ConcurrentHashMap<>();
    expectedResponse.put(new ConfigurationId(LOGISTIC_CENTER_ID, ORDERS_PER_PALLET_RATIO), ORDERS_PER_PALLET_RATIO_VALUE);
    expectedResponse.put(new ConfigurationId(LOGISTIC_CENTER_ID, UNITS_PER_ORDER_RATIO), UNITS_PER_ORDER_RATIO_VALUE);
    expectedResponse.put(new ConfigurationId(LOGISTIC_CENTER_ID, UNITS_PER_TOTE_RATIO), UNITS_PER_TOTE_RATIO_NEW_VALUE);

    //WHEN
    configurationUseCase.save(USER_ID, LOGISTIC_CENTER_ID, configsToSave);
    //THEN
    expectedResponse.forEach(
        (id, value) -> {
          final Optional<Configuration> optionalConfiguration = configurationRepository.findById(id);
          assertTrue(optionalConfiguration.isPresent(), "Configuration not found");
          final Configuration configuration = optionalConfiguration.get();
          assertEquals(value, configuration.getValue());
        }
    );
  }

}
