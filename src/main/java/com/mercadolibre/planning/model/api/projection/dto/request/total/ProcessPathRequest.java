package com.mercadolibre.planning.model.api.projection.dto.request.total;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.util.List;
import lombok.Value;

@Value
public class ProcessPathRequest {
  ProcessPath name;
  List<Quantity> quantity;
}
