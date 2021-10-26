package com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.web.controller.projection.request.ProjectionType;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static java.time.format.DateTimeFormatter.ofPattern;

@Value
@Builder
@Slf4j
public class CptProjectionInput {

    private static final int CYCLE_TIME_DEFAULT = 240;

    private Workflow workflow;

    private String logisticCenterId;

    private Map<ZonedDateTime, Integer> capacity;

    private List<GetPlanningDistributionOutput> planningUnits;

    private ZonedDateTime dateFrom;

    private ZonedDateTime dateTo;

    private List<Backlog> backlog;

    private ProjectionType projectionType;

    private Map<ZonedDateTime, Configuration> configurationByDateOut;

    public long getCycleTime(final ZonedDateTime dateOut) {

        final DateTimeFormatter dateFormat = ofPattern("yyyy-MM-dd'T'HH:mm':00'");

        for (final Map.Entry<ZonedDateTime, Configuration> mapItemConfig :
                configurationByDateOut.entrySet()) {
            final boolean found = mapItemConfig.getKey().format(dateFormat)
                    .equals(dateOut.format(dateFormat));

            log.info("DateOutKey [{}] equal DateOutArg [{}]",
                    mapItemConfig.getKey().format(dateFormat),
                    dateOut.format(dateFormat));

            if (found) {
                return mapItemConfig.getValue().getValue();
            }
        }

        log.error("ProcessingTime not found to dateOut [{}], return default value (0)",
                dateOut.format(dateFormat));

        return CYCLE_TIME_DEFAULT;
    }
}
