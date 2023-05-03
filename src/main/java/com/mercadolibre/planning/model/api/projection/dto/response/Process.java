package com.mercadolibre.planning.model.api.projection.dto.response;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import java.util.List;
import lombok.Value;

@Value
public class Process {
    ProcessName name;
    List<Sla> sla;
}
