package com.mercadolibre.planning.model.api.domain.usecase.backlog;

import static com.mercadolibre.planning.model.api.domain.entity.BacklogGrouper.DATE_IN;
import static com.mercadolibre.planning.model.api.domain.entity.BacklogGrouper.DATE_OUT;
import static com.mercadolibre.planning.model.api.domain.entity.BacklogGrouper.PATH;
import static com.mercadolibre.planning.model.api.domain.entity.BacklogGrouper.WORKFLOW;

import com.mercadolibre.planning.model.api.domain.entity.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    public Optional<Path> getPath() {
      Optional<String> path = Optional.ofNullable(key.get(PATH.getName()));
      return path.map(Path::from);
    }

    public Optional<Instant> getDateIn() {
      Optional<String> date = Optional.of(key.get(DATE_IN.getName()));
      return date.map(Instant::parse);
    }

    public Optional<Instant> getDateOut() {
      Optional<String> date = Optional.of(key.get(DATE_OUT.getName()));
      return date.map(Instant::parse);
    }

    public Optional<String> getWorkflow() {
      return Optional.ofNullable(key.get(WORKFLOW.getName()));
    }
  }

}
