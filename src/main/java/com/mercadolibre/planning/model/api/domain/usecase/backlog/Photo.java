package com.mercadolibre.planning.model.api.domain.usecase.backlog;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Class PhotoBacklog is used as currentPhotoBacklog and Photo in backlogHistorical.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Photo {
  Instant takenOn;

  List<Group> groups;

  /**
   * Values per take_out.
   */
  @AllArgsConstructor
  @NoArgsConstructor
  @Data
  public static class Group {
    Map<String, String> key;

    int total;

    int accumulatedTotal;
  }

}
