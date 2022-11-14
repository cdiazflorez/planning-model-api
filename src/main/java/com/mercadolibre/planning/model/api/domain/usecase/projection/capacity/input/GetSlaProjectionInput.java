package com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.web.controller.projection.request.ProjectionType;
import com.mercadolibre.planning.model.api.web.controller.projection.request.QuantityByDate;
import com.mercadolibre.planning.model.api.web.controller.projection.request.Source;
import com.mercadolibre.planning.model.api.web.controller.simulation.Simulation;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.Value;

@Value
public class GetSlaProjectionInput {

    private Workflow workflow;

    private String warehouseId;

    private ProjectionType type;

    private List<ProcessName> processName;

    private ZonedDateTime dateFrom;

    private ZonedDateTime dateTo;

    private List<QuantityByDate> backlog;

    private String timeZone;

    private Source source;

    private List<Simulation> simulations;

    private boolean applyDeviation;

    private ZonedDateTime viewDate;

}
