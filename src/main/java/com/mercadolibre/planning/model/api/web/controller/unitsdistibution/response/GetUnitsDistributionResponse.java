package com.mercadolibre.planning.model.api.web.controller.unitsdistibution.response;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import lombok.Builder;
import lombok.Value;


import java.time.ZonedDateTime;

@Value
@Builder
public class GetUnitsDistributionResponse {

   long id;

   String logisticCenterId;

   ZonedDateTime date;

   String processName;

   String area;

   Double quantity;

   MetricUnit quantityMetricUnit;
}
