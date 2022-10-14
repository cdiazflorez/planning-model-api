package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Date;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class ProcessingDistributionViewImpl implements ProcessingDistributionView {

    private final long id;

    private final Date date;

    private final ProcessPath processPath;

    private final ProcessName processName;

    private final long quantity;

    private final MetricUnit quantityMetricUnit;

    private final ProcessingType type;

}
