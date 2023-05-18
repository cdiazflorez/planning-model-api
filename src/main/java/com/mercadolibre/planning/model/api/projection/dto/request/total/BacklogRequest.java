package com.mercadolibre.planning.model.api.projection.dto.request.total;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class BacklogRequest {
  @JsonProperty("process_path")
  List<ProcessPathRequest> processPath;
}
