package com.mercadolibre.planning.model.api.projection.dto.response;

import java.util.List;
import lombok.Value;

@Value
public class Backlog {
    List<Process> process;
}
