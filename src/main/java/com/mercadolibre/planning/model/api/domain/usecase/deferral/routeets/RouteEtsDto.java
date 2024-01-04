package com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets;

import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteEtsDto {

  private String id;

  private String from;

  private String canalization;

  private String serviceId;

  private Map<String, List<DayDto>> fixedEtsByDay;

  private Date dateCreated;

  private Date lastUpdated;

  private String statusRoute;
}
