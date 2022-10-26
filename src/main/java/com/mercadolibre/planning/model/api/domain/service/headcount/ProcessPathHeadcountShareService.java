package com.mercadolibre.planning.model.api.domain.service.headcount;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ProcessPathHeadcountShareService {
  private final GetForecastUseCase getForecastUseCase;

  private final ProcessPathShareGateway processPathShareGateway;

  public Map<ProcessPath, Map<ProcessName, Map<Instant, Double>>> getHeadcountShareByProcessPath(
      final String logisticCenterId,
      final Workflow workflow,
      final Set<ProcessName> processes,
      final Instant dateFrom,
      final Instant dateTo,
      final Instant viewDate
  ) {
    final var forecastIds = getForecastIds(logisticCenterId, workflow, dateFrom, dateTo, viewDate);
    final var shares = processPathShareGateway.getProcessPathHeadcountShare(
        processes,
        dateFrom,
        dateTo,
        forecastIds
    );

    return shares.stream()
        .collect(Collectors.groupingBy(
            ShareAtProcessPathAndProcessAndDate::getProcessPath,
            Collectors.groupingBy(
                ShareAtProcessPathAndProcessAndDate::getProcessName,
                Collectors.toMap(
                    ShareAtProcessPathAndProcessAndDate::getDate,
                    ShareAtProcessPathAndProcessAndDate::getShare
                )
            )
        ));
  }

  private List<Long> getForecastIds(
      final String logisticCenterId,
      final Workflow workflow,
      final Instant dateFrom,
      final Instant dateTo,
      final Instant viewDate
  ) {
    return getForecastUseCase.execute(new GetForecastInput(
        logisticCenterId,
        workflow,
        ZonedDateTime.ofInstant(dateFrom, ZoneOffset.UTC),
        ZonedDateTime.ofInstant(dateTo, ZoneOffset.UTC),
        viewDate
    ));
  }

  @Value
  public static class ShareAtProcessPathAndProcessAndDate {
    ProcessPath processPath;

    ProcessName processName;

    Instant date;

    Double share;
  }

  /**
   * ProcessPathShareGateway, knows how to calculate or retrieve the headcount division between each ProcessPath present in the logistic
   *    center for the required processes and dates.
   */
  public interface ProcessPathShareGateway {
    List<ShareAtProcessPathAndProcessAndDate> getProcessPathHeadcountShare(
        Set<ProcessName> processNames,
        Instant dateFrom,
        Instant dateTo,
        List<Long> forecastIds
    );
  }

}
