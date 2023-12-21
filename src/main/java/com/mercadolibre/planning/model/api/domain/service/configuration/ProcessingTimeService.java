package com.mercadolibre.planning.model.api.domain.service.configuration;

import com.mercadolibre.planning.model.api.domain.service.configuration.SlaProcessingTimes.SlaProperties;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class ProcessingTimeService {

  private OutboundProcessingTimeGateway outboundProcessingTimeGateway;

  private FuryConfigServiceGateway furyConfigServiceGateway;

  @Transactional
  public SlaProcessingTimes getProcessingTimeForCptInDateRange(
      final String logisticCenterId,
      final Instant dateFrom,
      final Instant dateTo,
      final ZoneId zoneId
  ) {

    final Stream<SlaProperties> slaProperties = outboundProcessingTimeGateway.getOutboundProcessingTimeByCptInRange(
        logisticCenterId,
        dateFrom,
        dateTo,
        zoneId
    );

    return toSlaProcessingTimes(logisticCenterId, slaProperties);
  }

  @Transactional
  public List<DayAndHourProcessingTime> updateProcessingTimeForCptsByLogisticCenter(final String logisticCenterId) {

    final List<DayAndHourProcessingTime> latestRouteEtsProcessingTimes =
        outboundProcessingTimeGateway.getOutboundProcessingTimeByLogisticCenterFromRouteClient(logisticCenterId);

    return outboundProcessingTimeGateway.updateOutboundProcessingTimesForLogisticCenter(logisticCenterId, latestRouteEtsProcessingTimes);
  }

  private SlaProcessingTimes toSlaProcessingTimes(
      final String logisticCenterId,
      final Stream<SlaProperties> slaProperties
  ) {

    final int defaultProcessingTime = furyConfigServiceGateway.getProcessingTime(logisticCenterId);

    return new SlaProcessingTimes(
        defaultProcessingTime,
        slaProperties
            .sorted(Comparator.comparing(SlaProcessingTimes.SlaProperties::sla))
            .toList()
    );
  }

  /**
   * Interface to get and update the outbound processing times.
   */
  public interface OutboundProcessingTimeGateway {

    Stream<SlaProperties> getOutboundProcessingTimeByCptInRange(
        String logisticCenterId,
        Instant dateFrom,
        Instant dateTo,
        ZoneId zoneId
    );

    List<DayAndHourProcessingTime> getOutboundProcessingTimeByLogisticCenterFromRouteClient(String logisticCenterId);

    List<DayAndHourProcessingTime> updateOutboundProcessingTimesForLogisticCenter(
        String logisticCenterId,
        List<DayAndHourProcessingTime> processingTimes
    );
  }

  /**
   * Interface to get the default processing times from Fury config service.
   */
  public interface FuryConfigServiceGateway {

    int getProcessingTime(String logisticCenterId);
  }
}
