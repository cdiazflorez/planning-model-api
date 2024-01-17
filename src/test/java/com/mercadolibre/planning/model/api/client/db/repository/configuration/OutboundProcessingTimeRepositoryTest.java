package com.mercadolibre.planning.model.api.client.db.repository.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mercadolibre.planning.model.api.domain.entity.configuration.OutboundProcessingTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ActiveProfiles("development")
class OutboundProcessingTimeRepositoryTest {

  private static final String LOGISTIC_CENTER_ID = "COCU01";

  private static final String ETD_DAY = "thursday";

  private static final String ETD_HOUR = "2000";

  private static final Instant DATE_CREATED = Instant.parse("2023-11-17T22:30:00Z");

  private static final String SQL_RESOURCE = "/sql/processingtime/load_outbound_processing_time.sql";

  @Autowired
  private OutboundProcessingTimeRepository outboundProcessingTimeRepository;

  public List<OutboundProcessingTime> createOutboundProcessingTimeList() {

    return List.of(
        new OutboundProcessingTime(LOGISTIC_CENTER_ID, ETD_DAY, ETD_HOUR, 4, true),
        new OutboundProcessingTime(LOGISTIC_CENTER_ID, "wednesday", "2100", 2, true),
        new OutboundProcessingTime(LOGISTIC_CENTER_ID, "friday", "2300", 3, true)
    );
  }

  @Test
  @Sql(SQL_RESOURCE)
  void testUpdateIsActiveAndDateUpdated() {
    //GIVEN

    //WHEN
    outboundProcessingTimeRepository.deactivateByLogisticCenterAndCpt(LOGISTIC_CENTER_ID, ETD_DAY, ETD_HOUR);
    final Optional<OutboundProcessingTime> updatedRecord = outboundProcessingTimeRepository.findById(3L);

    //THEN
    assertTrue(updatedRecord.isPresent());
    assertFalse(updatedRecord.get().isActive());
    assertEquals(DATE_CREATED, updatedRecord.get().getDateCreated());
    assertTrue(updatedRecord.get().getDateUpdated().isAfter(DATE_CREATED));
  }

  @Test
  @Sql(SQL_RESOURCE)
  void testUpdateAllIsActiveAndDateUpdatedForCptsInLogisticCenter() {
    //GIVEN

    //WHEN
    outboundProcessingTimeRepository.deactivateAllByLogisticCenter(LOGISTIC_CENTER_ID);

    final List<OutboundProcessingTime> updatedRecords = outboundProcessingTimeRepository.findAllById(List.of(3L, 4L));

    //THEN
    assertFalse(updatedRecords.isEmpty());
    assertFalse(updatedRecords.get(0).isActive());
    assertFalse(updatedRecords.get(1).isActive());
    assertEquals(2, updatedRecords.size());
  }

  @Test
  @Sql(SQL_RESOURCE)
  void testSaveProcessingTime() {
    //GIVEN
    final String logisticCenterId = LOGISTIC_CENTER_ID;
    final String etdDay = ETD_DAY;
    final String etdHour = ETD_HOUR;
    final Instant dateCreated = Instant.now().truncatedTo(ChronoUnit.MINUTES);
    final int etdProcessingTime = 4;

    final OutboundProcessingTime processingTime = new OutboundProcessingTime(
        logisticCenterId,
        etdDay,
        etdHour,
        etdProcessingTime,
        true
    );

    //WHEN
    final OutboundProcessingTime savedRecord = outboundProcessingTimeRepository.save(processingTime);

    //THEN
    assertEquals(logisticCenterId, savedRecord.getLogisticCenterID());
    assertEquals(etdDay, savedRecord.getEtdDay());
    assertEquals(etdHour, savedRecord.getEtdHour());
    assertEquals(etdProcessingTime, savedRecord.getEtdProcessingTime());
    assertEquals(dateCreated, savedRecord.getDateCreated().truncatedTo(ChronoUnit.MINUTES));
    assertEquals(dateCreated, savedRecord.getDateUpdated().truncatedTo(ChronoUnit.MINUTES));
    assertTrue(savedRecord.isActive());
  }

  @Test
  @Sql(SQL_RESOURCE)
  void testSaveAll() {
    // GIVEN
    final List<OutboundProcessingTime> processingTimes = createOutboundProcessingTimeList();

    // WHEN
    final List<OutboundProcessingTime> result = outboundProcessingTimeRepository.saveAll(processingTimes);

    //THEN
    assertEquals(processingTimes, result);
  }

  @Test
  @Sql(SQL_RESOURCE)
  void testPurgeOldCptProcessingTimesRecords() {
    //GIVEN
    final long countBeforePurge = outboundProcessingTimeRepository.count();
    final Instant fourWeeksAgo = Instant.from(Instant.now().minus(30, ChronoUnit.DAYS));

    //WHEN
    outboundProcessingTimeRepository.purgeOldRecords(fourWeeksAgo);
    final long countAfterPurge = outboundProcessingTimeRepository.count();
    //THEN
    assertEquals(countBeforePurge - 1, countAfterPurge);
  }

  @Test
  @Sql(SQL_RESOURCE)
  void testFindByLogisticCenterAndIsActiveTrue() {
    // GIVEN
    final String logisticCenterId = LOGISTIC_CENTER_ID;

    // WHEN
    final List<OutboundProcessingTime> result = outboundProcessingTimeRepository.findByLogisticCenterAndIsActive(logisticCenterId);

    // THEN
    assertFalse(result.isEmpty());
    assertTrue(result.stream().allMatch(OutboundProcessingTime::isActive));
    assertTrue(result.stream().allMatch(processingTime -> processingTime.getLogisticCenterID().equals(logisticCenterId)));
    assertTrue(
        result.stream().anyMatch(processingTime -> ETD_DAY.equals(processingTime.getEtdDay())
            && ETD_HOUR.equals(processingTime.getEtdHour()))
    );
  }
}
