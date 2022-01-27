package com.mercadolibre.planning.model.api.gateway;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.BacklogPhoto;

import java.time.Instant;
import java.util.List;

public interface BacklogGateway {
    List<BacklogPhoto> getCurrentBacklog(String warehouseId,
                                         List<Workflow> workflows,
                                         List<String> steps,
                                         Instant slaFrom,
                                         Instant slaTo,
                                         List<String> groupingFields);
}
