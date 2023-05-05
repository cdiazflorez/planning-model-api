package com.mercadolibre.planning.model.api.projection.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import java.util.List;
import lombok.Value;

@Value
public class Process {

    ProcessName name;
    int total;
}
