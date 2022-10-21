package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.util.Date;

public interface HeadcountProductivityView {

    ProcessName getProcessName();

    long getProductivity();

    MetricUnit getProductivityMetricUnit();

    Date getDate();

    int getAbilityLevel();

    ProcessPath getProcessPath();
}
