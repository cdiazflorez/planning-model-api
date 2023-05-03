package com.mercadolibre.planning.model.api.projection.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PlannedUnit {
    @JsonProperty("process_path")
    List<ProcessPathRequest> processPath;
}
