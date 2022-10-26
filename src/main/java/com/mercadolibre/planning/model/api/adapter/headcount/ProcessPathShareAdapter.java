package com.mercadolibre.planning.model.api.adapter.headcount;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.service.headcount.ProcessPathHeadcountShareService.ProcessPathShareGateway;
import com.mercadolibre.planning.model.api.domain.service.headcount.ProcessPathHeadcountShareService.ShareAtProcessPathAndProcessAndDate;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ProcessPathShareAdapter implements ProcessPathShareGateway {

  private final ProcessingDistributionRepository repository;


  @Override
  public List<ShareAtProcessPathAndProcessAndDate> getProcessPathHeadcountShare(
      final Set<ProcessName> processNames,
      final Instant dateFrom,
      final Instant dateTo,
      final List<Long> forecastIds
  ) {
    return repository.getProcessPathHeadcountShare(
        processNames.stream().map(ProcessName::getName).collect(Collectors.toList()),
        dateFrom,
        dateTo,
        forecastIds
    )
        .stream()
        .map(v -> new ShareAtProcessPathAndProcessAndDate(v.getProcessPath(), v.getProcessName(), v.getDate(), v.getShare()))
        .collect(Collectors.toList());
  }

  /**
   * ShareView represents the result of the query that calculates the ProcessPath share for each date and Process.
   */
  public interface ShareView {
    ProcessPath getProcessPath();

    ProcessName getProcessName();

    Instant getDate();

    Double getShare();
  }
}
