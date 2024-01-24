package com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.step;

import static com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlannedUnitsService.Input;
import static com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.step.Options.APPLY_DATE_OUT_FILTERING;
import static com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.step.Options.VIEW_DATE;
import static com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.step.Tags.DATE_OUT;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlannedUnitsService.TaggedUnit;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DateOutFilterBuilder {

  private static final String APPLY = "true";

  private final DateOutsGateway dateOutsGateway;

  private static Instant getViewDate(final Map<String, String> options) {
    return Optional.ofNullable(options)
        .map(opt -> opt.get(VIEW_DATE))
        .map(Instant::parse)
        .orElse(Instant.now());
  }

  public UnaryOperator<Stream<TaggedUnit>> build(final Input input) {
    return taggedUnits -> {
      if (!APPLY.equalsIgnoreCase(input.options().get(APPLY_DATE_OUT_FILTERING))) {
        return taggedUnits;
      }

      final var dateOutsToFilter = dateOutsGateway.getDateOutsToFilter(
              input.networkNode(),
              input.workflow(),
              getViewDate(input.options())
          )
          .stream()
          .map(Instant::toString)
          .collect(Collectors.toSet());

      return taggedUnits.filter(taggedUnit -> !dateOutsToFilter.contains(taggedUnit.tags().get(DATE_OUT))
      );
    };
  }

  public interface DateOutsGateway {
    List<Instant> getDateOutsToFilter(String warehouseId, Workflow workflow, Instant viewDate);
  }

}
