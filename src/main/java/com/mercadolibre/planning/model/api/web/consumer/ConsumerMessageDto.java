package com.mercadolibre.planning.model.api.web.consumer;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ConsumerMessageDto(
    @JsonProperty("id")
    String id,
    @JsonAlias({ "msg", "data" })
    ProcessingTimeToUpdate data,
    @JsonProperty("publish_time")
    Long publishTime
) {
  public record ProcessingTimeToUpdate(
      String id,
      @JsonProperty("from")
      String logisticCenterId,
      @JsonProperty("event_type")
      String eventType
  ) {
  }
}
