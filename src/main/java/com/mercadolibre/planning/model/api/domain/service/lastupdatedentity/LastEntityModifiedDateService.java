package com.mercadolibre.planning.model.api.domain.service.lastupdatedentity;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class LastEntityModifiedDateService {

  private final GetLastModifiedDateGateway getLastModifiedDateGateway;


  public LastModifiedDates getLastEntityDateModified(final String logisticCenterId,
                                                     final Workflow workflow,
                                                     final Set<EntityType> entityTypes,
                                                     final Instant viewDate) {


    final Instant lastDateStaffingCreated = getLastModifiedDateGateway
        .getLastDateStaffingCreated(logisticCenterId, workflow, viewDate);

    final Map<EntityType, Instant> lastDateEntitiesCreated = entityTypes == null || entityTypes.isEmpty()
        ? Collections.emptyMap()
        : getLastModifiedDateGateway.getLastDateEntitiesCreated(logisticCenterId, workflow, entityTypes, lastDateStaffingCreated);

    return new LastModifiedDates(lastDateStaffingCreated, lastDateEntitiesCreated);
  }

  /**
   * Interface to get last modified date of staffing plan and editions.
   */
  public interface GetLastModifiedDateGateway {

    Instant getLastDateStaffingCreated(String logisticCenterId,
                                       Workflow workflow,
                                       Instant viewDate);

    Map<EntityType, Instant> getLastDateEntitiesCreated(String logisticCenterId,
                                                        Workflow workflow,
                                                        Set<EntityType> entityTypes,
                                                        Instant staffingDateCreated);
  }

}
