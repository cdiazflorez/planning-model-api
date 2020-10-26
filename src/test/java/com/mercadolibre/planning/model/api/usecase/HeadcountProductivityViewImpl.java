package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.HeadcountProductivityView;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import lombok.AllArgsConstructor;

import java.util.Date;

@AllArgsConstructor
public class HeadcountProductivityViewImpl implements HeadcountProductivityView {

    private ProcessName processName;

    private long productivity;

    private MetricUnit productivityMetricUnit;

    private Date date;


    @Override
    public ProcessName getProcessName() {
        return processName;
    }

    @Override
    public long getProductivity() {
        return productivity;
    }

    @Override
    public MetricUnit getProductivityMetricUnit() {
        return productivityMetricUnit;
    }

    @Override
    public Date getDate() {
        return date;
    }
}
