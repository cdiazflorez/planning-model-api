package com.mercadolibre.planning.model.api.domain.service.configuration;

import com.mercadolibre.planning.model.api.domain.entity.configuration.OutboundProcessingTime;
import java.time.DayOfWeek;
import java.util.Locale;

public record DayAndHourProcessingTime(
    String logisticCenterID,
    String etdDay,
    String etdHour,
    int etdProcessingTime
) {
  public DayAndHourProcessingTime(OutboundProcessingTime data) {
    this(data.getLogisticCenterID(), data.getEtdDay(), data.getEtdHour(), data.getEtdProcessingTime());
  }

  public OutboundProcessingTime toActiveOutboundProcessingTime() {
    return new OutboundProcessingTime(
        this.logisticCenterID(),
        this.etdDay(),
        this.etdHour(),
        this.etdProcessingTime(),
        true
    );
  }

  public DayOfWeek getDayOfWeek() {
    return DayOfWeek.valueOf(etdDay.toUpperCase(Locale.US));
  }
}
