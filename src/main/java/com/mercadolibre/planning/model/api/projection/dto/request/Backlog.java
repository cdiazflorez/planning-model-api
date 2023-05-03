package com.mercadolibre.planning.model.api.projection.dto.request;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Backlog {
    List<Process> process;
}
