package com.mercadolibre.planning.model.api.web.controller.deferral;

import java.time.Instant;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class Msg {
  @NotNull
  @Valid
  private DeferralMsg msg;

  @Data
  public static class DeferralMsg {
    @NotNull
    String warehouseId;
    @NotNull
    Instant lastUpdated;
    @NotNull
    List<Projection> projections;
  }

  @Data
  public static class Projection {
    @NotNull
    Instant estimatedTimeDeparture;
    @NotNull
    Instant payBefore;
    @NotNull
    DeferralStatusRequest deferralStatus;
  }
}


