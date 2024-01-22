package com.mercadolibre.planning.model.api.adapter.units;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningDistributionDynamicRepository;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlannedUnitsService.Input;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlannedUnitsService.TaggedUnit;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.step.PlanningDistributionSupplierBuilder.PlanningDistributionGateway;
import com.mercadolibre.planning.model.api.exception.InvalidArgumentException;
import com.mercadolibre.planning.model.api.util.DateUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

@AllArgsConstructor
public class PlanningDistributionAdapter implements PlanningDistributionGateway {

  private static final String MISSING_OPTIONS_MSG =
      "At least one of (date_in_from, date_out_from) and one of (date_in_to, date_out_to) are required as an option.";

  private final GetForecastUseCase getForecastUseCase;

  private final PlanningDistributionDynamicRepository planningDistributionRepository;

  private static Instant getDate(final Map<String, String> options, final String key) {
    return Optional.of(options)
        .map(opt -> opt.get(key))
        .map(Instant::parse)
        .orElse(null);
  }

  private static Set<ProcessPath> getProcessPaths(final Map<String, String> options) {
    final String pps = options.get("process_paths");
    return pps == null
        ? Collections.emptySet()
        : Arrays.stream(pps.split(","))
        .map(ProcessPath::of)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .filter(pp -> pp != ProcessPath.UNKNOWN)
        .collect(Collectors.toSet());
  }

  public Stream<TaggedUnit> get(final Input input) {
    final DateRanges dateRanges = new DateRanges(
        getDate(input.options(), "date_in_from"),
        getDate(input.options(), "date_in_to"),
        getDate(input.options(), "date_out_from"),
        getDate(input.options(), "date_out_to")
    );

    final var forecastIds = queryForecastIds(input, dateRanges);

    final var planningDistributions = getPlanningDistributions(input, forecastIds, dateRanges);

    return planningDistributions.stream()
        .map(pd -> new TaggedUnit(
                pd.getQuantity(),
                Map.of(
                    "date_in", pd.getDateIn().toString(),
                    "date_out", pd.getDateOut().toString(),
                    "process_path", pd.getProcessPath().toString()
                )
            )
        );
  }

  private Set<Long> queryForecastIds(final Input input, final DateRanges dates) {
    final var queryForecastIdDateFrom = DateUtils.min(dates.dateInFrom, dates.dateOutFrom);
    final var queryForecastIdDateTo = DateUtils.max(dates.dateInTo, dates.dateOutTo);

    if (queryForecastIdDateFrom.isEmpty() || queryForecastIdDateTo.isEmpty()) {
      throw new InvalidArgumentException(MISSING_OPTIONS_MSG);
    }

    final Instant viewDate = getDate(input.options(), "view_date");

    final List<Long> forecastIds = getForecastUseCase.execute(
        new GetForecastInput(
            input.networkNode(),
            input.workflow(),
            queryForecastIdDateFrom.get(),
            queryForecastIdDateTo.get(),
            viewDate
        )
    );

    return new HashSet<>(forecastIds);
  }

  private List<PlanDistribution> getPlanningDistributions(
      final Input input,
      final Set<Long> forecastIds,
      final DateRanges dateRanges
  ) {
    final List<PlanDistribution> dirtyPlanDistribution = planningDistributionRepository.findByForecastIdsAndDynamicFilters(
        dateRanges.dateInFrom,
        dateRanges.dateInTo,
        dateRanges.dateOutFrom,
        dateRanges.dateOutTo,
        getProcessPaths(input.options()),
        forecastIds
    );

    return removeDuplicatedData(dirtyPlanDistribution);
  }

  private List<PlanDistribution> removeDuplicatedData(final List<PlanDistribution> duplicatedPlanning) {
    final Map<Pair<Instant, Instant>, Long> dateByForecastId = new ConcurrentHashMap<>();
    final List<PlanDistribution> planning = new ArrayList<>();

    duplicatedPlanning.forEach(elem -> {
      final Pair<Instant, Instant> dateOutDateIn = new ImmutablePair<>(
          elem.getDateOut(), elem.getDateIn()
      );

      final Long selectedForecastForDateInDateOut = dateByForecastId.get(dateOutDateIn);
      if (selectedForecastForDateInDateOut == null) {
        dateByForecastId.put(dateOutDateIn, elem.getForecastId());
        planning.add(elem);
      } else if (selectedForecastForDateInDateOut == elem.getForecastId()) {
        planning.add(elem);
      }
    });
    return planning;
  }

  private record DateRanges(Instant dateInFrom, Instant dateInTo, Instant dateOutFrom, Instant dateOutTo) {
  }
}
