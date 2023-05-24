package com.mercadolibre.planning.model.api.projection.dto.request;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.time.Instant;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Value;

@Value
public class Process {

    ProcessName name;

    List<ProcessPathDetail> processPath;

    @Value
    public static class ProcessPathDetail {
        ProcessPath name;

        List<QuantityByDate> quantity;
    }

    @Value
    public static class QuantityByDate {

        @NotNull
        Instant dateOut;

        int total;
    }
}
