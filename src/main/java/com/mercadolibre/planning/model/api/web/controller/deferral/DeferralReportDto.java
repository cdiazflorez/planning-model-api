package com.mercadolibre.planning.model.api.web.controller.deferral;

import com.mercadolibre.planning.model.api.domain.usecase.deferral.DeferralType;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class DeferralReportDto {
  List<DeferralTime> deferralTime;

  @Value
  @AllArgsConstructor
  public static class DeferralTime {
    Instant date;
    List<StatusBySla> sla;

    @Value
    @AllArgsConstructor
    public static class StatusBySla {
      Instant date;
      DeferralType status;
    }
  }


}
